package staport.finance.gBanque.validation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import staport.finance.gBanque.AbstractIntegrationTest;
import staport.finance.gBanque.auth.services.AuthCookieService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.repositories.UserRepository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bascule manuelle du statut (administrateur système) : cohérence sur tous les types d'opération qui s'y prêtent.
 *
 * <ul>
 *   <li>validée ET rapprochée → la transaction liée est supprimée, l'opération redevient « en cours » ;</li>
 *   <li>validée sans transaction (réglée à la main) → bascule libre, comme avant ;</li>
 *   <li>transaction dans un rapprochement définitivement clôturé → refus (400), rien ne change ;</li>
 *   <li>« en cours » → validée : inchangé.</li>
 * </ul>
 *
 * <p>Les lignes sont insérées en SQL (colonnes obligatoires remplies automatiquement, clés étrangères suspendues sauf la
 * banque, réelle). Non couverts ici : les échéances de leasing et de CMT, dont le graphe de dépendances (contrat, crédit)
 * est trop lourd à fabriquer ; leur bascule reçoit le même code que les autres.
 */
class BasculeStatutRapprocheIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MotDePasse-Test-1";
    private static final AtomicLong IDS_BANQUE = new AtomicLong(910_000);
    /** Suffixe unique des valeurs texte générées (colonnes à contrainte d'unicité : numéro de compte, identifiant de paiement…). */
    private static final AtomicLong UNICITE = new AtomicLong();

    /** Un type d'opération : table, colonne de statut, valeurs, source de transaction et route de bascule. */
    record Cas(String nom, String table, String colonneStatut, String valide, String initial, String source, String route) {
        @Override public String toString() { return nom; }
    }

    static Stream<Cas> cas() {
        return Stream.of(
                new Cas("Agios", "agios", "status", "PAYE", "PREVU", "AGIOS", "/agios/{id}/toggle-status"),
                new Cas("Commission", "commission", "status", "PAYE", "PREVU", "COMMISSION", "/commissions/{id}/toggle-status"),
                new Cas("Décompte", "decompte", "status", "PAYE", "PREVU", "DECOMPTES", "/decomptes/{id}/toggle"),
                new Cas("Droit constaté", "droit_constate", "status", "PAYE", "PREVU", "DROIT_CONSTATE", "/droitsConstate/{id}/status/toggle"),
                new Cas("Factoring", "factoring", "status", "PAYE", "PREVU", "FACTORING", "/factoring/{id}/toggleStatus"),
                new Cas("Frais bancaire", "frais_bancaire", "status", "PAYE", "PREVU", "FRAIS_BANCAIRE", "/frais-bancaire/{id}/toggle-status"),
                new Cas("Impayée", "impayee", "status", "PAYE", "PREVU", "IMPAYEE", "/impayees/{id}/toggle-status"),
                new Cas("Impôt / Taxe", "impot_et_taxe", "status", "PAYE", "PREVU", "IMPOT_TAXE", "/impots-taxes/{id}/toggle-status"),
                new Cas("Opération erronée", "operation_errone", "status", "PAYE", "PREVU", "OPERATION_ERRONE", "/operation-errone/{id}/toggle-status"),
                new Cas("Préfinancement", "prefinancement", "status", "PAYE", "PREVU", "PREFINANCEMENT", "/prefinancements/{id}/status/toggle"),
                new Cas("Prélèvement", "prelevement", "status", "PAYE", "PREVU", "PRELEVEMENT", "/prelevements/{id}/toggle-status"),
                new Cas("Provision", "provision", "status", "PAYE", "PREVU", "PROVISION", "/provision/{id}/toggle-status"),
                new Cas("Restitution", "restitution", "status", "PAYE", "PREVU", "RESTITUTION", "/restitutions/{id}/toggle-status"),
                new Cas("Chèque", "cheques", "statut", "REGLE", "ENCOURS", "CHEQUE", "/cheques/toggle/{id}"),
                new Cas("LCN", "lcns", "statut", "REGLE", "ENCOURS", "LCN_DECAISSEMENT", "/lcns/toggle/{id}"),
                new Cas("LCN (escompte)", "lcns", "statut", "REGLE", "ENCOURS", "LCN_ESCOMPTE", "/lcns/toggle/{id}"),
                new Cas("Mise à disposition", "mises_a_disposition", "statut", "REGLE", "ENCOURS", "MiseADisposition", "/misesADisposition/toggle/{id}"),
                new Cas("Virement", "virements", "statut", "REGLE", "ENCOURS", "Virement", "/virements/toggle/{id}"),
                new Cas("Virement international", "virements_internationaux", "statut", "REGLE", "ENCOURS", "VIREMENT_INTERNATIONAL", "/virements-internationaux/toggle/{id}")
        );
    }

    /** Classe d'entité enregistrée dans le journal d'audit pour chaque table. */
    private static final Map<String, String> ENTITES = Map.ofEntries(
            Map.entry("agios", "Agios"), Map.entry("commission", "Commission"), Map.entry("decompte", "Decompte"),
            Map.entry("droit_constate", "DroitConstate"), Map.entry("factoring", "Factoring"),
            Map.entry("frais_bancaire", "FraisBancaire"), Map.entry("impayee", "Impayee"),
            Map.entry("impot_et_taxe", "ImpotEtTaxe"), Map.entry("operation_errone", "OperationErrone"),
            Map.entry("prefinancement", "Prefinancement"), Map.entry("prelevement", "Prelevement"),
            Map.entry("provision", "Provision"), Map.entry("restitution", "Restitution"), Map.entry("cheques", "Cheque"),
            Map.entry("lcns", "Lcn"), Map.entry("mises_a_disposition", "MiseADisposition"), Map.entry("virements", "Virement"),
            Map.entry("virements_internationaux", "VirementInternational"));

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Cookie admin;

    // ── Scénarios ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} : validée et rapprochée → transaction supprimée, retour en cours")
    @MethodSource("cas")
    @DisplayName("Retour en cours d'une opération rapprochée")
    void rapprochee_annuleLaTransaction(Cas c) throws Exception {
        long banque = nouvelleBanque();
        long id = ligne(c, c.valide(), banque);
        long transaction = transaction(c, id, banque, "EN_COURS");

        basculer(c, id).andExpect(status().is2xxSuccessful());

        assertThat(statut(c, id)).isEqualTo(c.initial());
        assertThat(compter("transaction", "id = " + transaction)).isZero();
    }

    @ParameterizedTest(name = "{0} : validée sans transaction → bascule libre")
    @MethodSource("cas")
    @DisplayName("Opération validée à la main, sans transaction")
    void sansTransaction_basculeLibre(Cas c) throws Exception {
        long id = ligne(c, c.valide(), nouvelleBanque());

        basculer(c, id).andExpect(status().is2xxSuccessful());

        assertThat(statut(c, id)).isEqualTo(c.initial());
    }

    @ParameterizedTest(name = "{0} : rapprochement clôturé définitivement → refus, rien ne change")
    @MethodSource("cas")
    @DisplayName("Rapprochement clôturé définitivement")
    void clotureDefinitive_refus(Cas c) throws Exception {
        long banque = nouvelleBanque();
        long id = ligne(c, c.valide(), banque);
        long transaction = transaction(c, id, banque, "CLOTURE_DEFINITIVE");

        basculer(c, id).andExpect(status().isBadRequest());

        assertThat(statut(c, id)).isEqualTo(c.valide());
        assertThat(compter("transaction", "id = " + transaction)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0} : en cours → validée, inchangé")
    @MethodSource("cas")
    @DisplayName("Passage en validé, inchangé")
    void enCours_vaValide(Cas c) throws Exception {
        long id = ligne(c, c.initial(), nouvelleBanque());

        basculer(c, id).andExpect(status().is2xxSuccessful());

        assertThat(statut(c, id)).isEqualTo(c.valide());
    }

    @ParameterizedTest(name = "{0} : la bascule est tracée dans l'audit")
    @MethodSource("cas")
    @DisplayName("Audit de la bascule")
    void bascule_estTraceeDansLAudit(Cas c) throws Exception {
        long id = ligne(c, c.initial(), nouvelleBanque());

        basculer(c, id).andExpect(status().is2xxSuccessful());

        assertThat(compter("log", "entity_name = '" + ENTITES.get(c.table()) + "' AND entity_id = " + id))
                .as("entrée du journal d'audit pour %s n° %d", c.nom(), id).isPositive();
    }

    @ParameterizedTest(name = "{0} : la suppression de la transaction liée est tracée dans l'audit")
    @MethodSource("cas")
    @DisplayName("Audit de la transaction supprimée")
    void retourEnCours_traceLaSuppressionDeLaTransaction(Cas c) throws Exception {
        long banque = nouvelleBanque();
        long id = ligne(c, c.valide(), banque);
        long transaction = transaction(c, id, banque, "EN_COURS");

        basculer(c, id).andExpect(status().is2xxSuccessful());

        assertThat(compter("log", "entity_name = 'Transaction' AND entity_id = " + transaction))
                .as("suppression de la transaction %d tracée", transaction).isPositive();
    }

    // ── Accès ──────────────────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.ResultActions basculer(Cas c, long id) throws Exception {
        return mockMvc.perform(patch(c.route().replace("{id}", String.valueOf(id))).cookie(admin()));
    }

    private Cookie admin() throws Exception {
        if (admin == null) {
            String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
            User user = new User(username, passwordEncoder.encode(PASSWORD), true);
            user.setSystemAdmin(true);
            userRepository.save(user);
            admin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getCookie(AuthCookieService.ACCESS_COOKIE);
        }
        return admin;
    }

    // ── Données ────────────────────────────────────────────────────────────────

    private long nouvelleBanque() {
        long id = IDS_BANQUE.incrementAndGet();
        insererLigne("banque", Map.of("id", id));
        return id;
    }

    private long ligne(Cas c, String statut, long banque) {
        Map<String, Object> v = new HashMap<>();
        v.put(c.colonneStatut(), statut);
        for (String colonneBanque : List.of("banque_id", "banque_encaissement_id")) {
            v.put(colonneBanque, banque);
        }
        if (List.of("prefinancement", "factoring", "droit_constate").contains(c.table())) {
            v.put("marche_id", insererLigne("marches", Map.of("banque_id", banque)));   // les mappeurs lisent le marché
        }
        return insererLigne(c.table(), v);
    }

    private long transaction(Cas c, long referent, long banque, String statutRapprochement) {
        Map<String, Object> rapprochement = new HashMap<>();
        rapprochement.put("banque_id", banque);
        rapprochement.put("statut", statutRapprochement);
        long idRapprochement = insererLigne("rapprochement", rapprochement);

        Map<String, Object> t = new HashMap<>();
        t.put("rapprochement_id", idRapprochement);
        t.put("transaction_source", c.source());
        t.put("referent", referent);
        t.put("banque_id", banque);
        return insererLigne("transaction", t);
    }

    private String statut(Cas c, long id) {
        return jdbc.queryForObject("SELECT " + c.colonneStatut() + " FROM " + c.table() + " WHERE id = " + id, String.class);
    }

    private long compter(String table, String condition) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + condition, Long.class);
    }

    /**
     * Insère une ligne minimale : les colonnes obligatoires sans valeur par défaut sont remplies d'une valeur plausible
     * (première valeur autorisée par un éventuel CHECK d'énumération), les valeurs données priment. Clés étrangères
     * suspendues pour l'insertion — seule la banque est réelle (les mappeurs la lisent).
     */
    private long insererLigne(String table, Map<String, Object> valeurs) {
        return jdbc.execute((Connection con) -> {
            try (Statement st = con.createStatement()) {
                st.execute("SET session_replication_role = replica");
                try {
                    Map<String, List<String>> enums = enumerations(st, table);
                    Map<String, String> colonnes = new LinkedHashMap<>();
                    try (ResultSet rs = st.executeQuery("SELECT column_name, data_type FROM information_schema.columns "
                            + "WHERE table_schema = 'public' AND table_name = '" + table + "' AND is_nullable = 'NO' "
                            + "AND column_default IS NULL AND is_identity = 'NO' ORDER BY ordinal_position")) {
                        while (rs.next()) {
                            colonnes.put(rs.getString(1), rs.getString(2));
                        }
                    }
                    Map<String, String> litteraux = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> e : valeurs.entrySet()) {
                        litteraux.put(e.getKey(), litteral(e.getValue()));
                    }
                    for (Map.Entry<String, String> col : colonnes.entrySet()) {
                        litteraux.computeIfAbsent(col.getKey(), n -> valeurParDefaut(n, col.getValue(), enums));
                    }
                    // colonnes données mais absentes de la table (ex. banque_encaissement_id sur une table sans cette colonne)
                    litteraux.keySet().removeIf(n -> !existe(st, table, n));
                    String sql = "INSERT INTO " + table + " (" + String.join(", ", litteraux.keySet()) + ") VALUES ("
                            + String.join(", ", litteraux.values()) + ") RETURNING id";
                    try (ResultSet rs = st.executeQuery(sql)) {
                        rs.next();
                        return rs.getLong(1);
                    }
                } finally {
                    st.execute("SET session_replication_role = DEFAULT");
                }
            }
        });
    }

    private static boolean existe(Statement st, String table, String colonne) {
        try (ResultSet rs = st.executeQuery("SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' "
                + "AND table_name = '" + table + "' AND column_name = '" + colonne + "'")) {
            return rs.next();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String litteral(Object valeur) {
        return valeur instanceof Number ? valeur.toString() : "'" + valeur.toString().replace("'", "''") + "'";
    }

    private static String valeurParDefaut(String colonne, String type, Map<String, List<String>> enums) {
        if (enums.containsKey(colonne)) {
            return litteral(enums.get(colonne).get(0));
        }
        return switch (type) {
            case "bigint", "integer", "smallint" -> colonne.startsWith("banque") ? "0" : "1";
            case "numeric" -> "100.00";
            case "date" -> "CURRENT_DATE";
            case "boolean" -> "false";
            case "bytea" -> "decode('00', 'hex')";
            default -> type.startsWith("timestamp") ? "now()" : "'X" + UNICITE.incrementAndGet() + "'";
        };
    }

    /** Colonne → valeurs autorisées, lues dans les contraintes CHECK de type « colonne IN (…) ». */
    private static Map<String, List<String>> enumerations(Statement st, String table) throws java.sql.SQLException {
        Map<String, List<String>> enums = new HashMap<>();
        Pattern colonne = Pattern.compile("\\((\\w+)\\)::text = ANY");
        Pattern valeur = Pattern.compile("'([^']*)'::character varying");
        try (ResultSet rs = st.executeQuery("SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                + "WHERE conrelid = 'public." + table + "'::regclass AND contype = 'c'")) {
            while (rs.next()) {
                String def = rs.getString(1);
                Matcher mc = colonne.matcher(def);
                if (!mc.find()) {
                    continue;
                }
                List<String> valeurs = new ArrayList<>();
                Matcher mv = valeur.matcher(def);
                while (mv.find()) {
                    valeurs.add(mv.group(1));
                }
                if (!valeurs.isEmpty()) {
                    enums.putIfAbsent(mc.group(1), valeurs);
                }
            }
        }
        return enums;
    }
}
