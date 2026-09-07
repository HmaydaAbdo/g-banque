# Architecture

> Conventions d'architecture backend applicables à tous les modules.

## Architecture d'un module

Chaque domaine métier suit la même structure :

```text
module/
├── controller/
├── dtos/
│   ├── request/
│   └── response/
├── entities/
├── exceptions/
├── mappers/
├── repositories/
├── services/
├── specifications/
└── metrics/
```

## Couches

### Entity

* Modèle de persistance uniquement.
* Jamais exposée par l'API.
* Aucune logique métier.

### Repository

* `JpaRepository` obligatoire.
* `JpaSpecificationExecutor` pour les listes filtrables.
* `@EntityGraph` pour les relations sérialisées.
* Jamais de logique métier.
* Ne jamais utiliser `spring.jpa.open-in-view`.

### Service

* Toute la logique métier.
* `@Transactional` sur les écritures.
* `@Transactional(readOnly = true)` sur les lectures.
* Les recherches d'entités sont centralisées dans des méthodes privées.

### Controller

* Thin Controller.
* DTO uniquement.
* Délègue toute la logique au Service.
* Jamais de Repository.

### DTO

* Java Records uniquement.
* Request / Response séparés.
* Validation Jakarta complète.

### Mapper

* `@Mapper(componentModel = "spring")`.
* Conversion uniquement.
* Jamais de logique métier.
* Jamais de Repository.

### Specification

* Toute liste filtrable utilise une Specification.
* Convention :

```java
XSpecification.searchByCriteria(XSearchCriteria)
```

### Exceptions

* Un `@RestControllerAdvice(assignableTypes = XController.class)` par module.
* `GlobalExceptionHandler` gère uniquement les erreurs imprévues.

---

# Organisation des packages (DDD)

* Un package racine par domaine métier.
* Sous-packages techniques internes uniquement (`controller`, `service`, `repository`, etc.).
* Jamais plusieurs domaines dans un package technique partagé.
* L'infrastructure (`auth`, `jwt`, `security`...) reste séparée des domaines métier.

---

# Encapsulation des domaines

* Un Repository appartient exclusivement à son domaine.
* Communication inter-domaines via le Service uniquement.
* Ne jamais injecter un Repository d'un autre domaine.
* Chaque Service constitue la façade publique de son domaine.

Exemple :

```text
LeasingService
      ↓
BanqueService
      ↓
BanqueRepository
```

---

# Modules générant des transactions — Règles de validation et synchronisation

## Règle 1 — La logique de validation appartient au module source

Lorsqu'un module métier génère des transactions bancaires (rapprochement), **la logique de validation doit vivre dans ce module**, pas dans le service générique `ValidationService`.

### Structure obligatoire

Pour tout module `Xxx` qui produit des transactions :

```text
leasing/
└── echeancesLeasing/
    ├── services/
    │   ├── interfaces/
    │   │   └── IXxxValidationService.java   ← interface du domaine
    │   └── XxxValidationService.java        ← implémentation
    ├── dto/
    │   ├── XxxTransactionRequestDTO.java    ← record Java
    │   └── XxxTransactionResponseDTO.java   ← record Java
    └── mapper/
        └── XxxTransactionMapper.java        ← MapStruct abstract class
```

### Responsabilités de `XxxValidationService`

* Récupérer les entités du **même domaine** (ses propres repositories).
* Construire la transaction via son `XxxTransactionMapper`.
* Attacher la transaction au rapprochement cible et la sauvegarder.
* Mettre à jour l'entité source (statut, dates) après persistance.

### Rôle de `ValidationController`

`ValidationController` reste le point d'entrée HTTP unique pour tous les rapprochements. Il **délègue** chaque endpoint au service de validation du module concerné via son interface :

```java
// Correct
@PostMapping("/process/echeance-leasing")
public ResponseEntity<LeasingTransactionResponseDTO> processLeasingTransaction(
        @Valid @RequestBody LeasingTransactionRequestDTO dto) {
    return new ResponseEntity<>(leasingValidationService.processLeasingTransaction(dto), CREATED);
}

// Interdit
validationService.processXxx(...); // logique métier du module Xxx dans ValidationService
```

### Ce que `ValidationService` ne doit jamais contenir

* Aucun repository d'un autre module.
* Aucune entité d'un autre module.
* Aucune logique métier propre à un module (ex : vérification du statut d'une échéance leasing).

---

## Règle 2 — Synchronisation Transaction ↔ Entité source

Lorsqu'une transaction est liée à une entité source (via `Transaction.referent` + `Transaction.transactionSource`), **toute modification de la transaction doit être propagée à l'entité source**.

### Mécanisme

* `Transaction.referent` contient l'ID de l'entité source.
* `Transaction.transactionSource` identifie le module (`LEASING`, `CMT`, etc.).
* `TransactionService.updateTransactionById()` délègue la synchronisation au service de validation du module détecté.

```java
if (TransactionSource.LEASING == saved.getTransactionSource() && saved.getReferent() != null) {
    leasingValidationService.syncEcheanceFromTransaction(
            saved.getReferent(), saved.getDateValeur(), saved.getDateOperations());
}
```

### Interface obligatoire

Tout `IXxxValidationService` doit exposer une méthode de synchronisation :

```java
void syncXxxFromTransaction(Long sourceId, LocalDate dateValeur, LocalDate dateOperation);
```

### Règle de blocage

Une entité source dont le statut est terminal (`PAYE`, `VALIDE`, etc.) **ne peut pas être modifiée** via cette synchronisation. La méthode `syncXxxFromTransaction` doit vérifier le statut et lever une exception si nécessaire, ou simplement ignorer la synchronisation si le statut est irréversible par design.

### Cohérence garantie

| Action | Effet |
|---|---|
| Validation d'une entité source | Transaction créée + entité mise à jour (statut + dates) |
| Modification directe de l'entité source (si PREVU) | Modification de l'entité uniquement |
| Modification de la transaction liée (update) | Propagation automatique vers l'entité source |

---

## Règle 3 — La logique de commission appartient au module source

Lorsqu'un module métier génère des commissions (leasing, cautions, virements, etc.), **la logique de calcul et de génération des commissions doit vivre dans ce module**, pas dans le module `commission`.

### Structure obligatoire

Pour tout module `Xxx` qui produit des commissions :

```text
xxx/
└── sousModule/
    └── services/
        ├── interfaces/
        │   └── IXxxCommissionService.java   ← optionnel si utilisé en dehors du module
        └── XxxCommissionService.java        ← logique de génération propre au domaine
```

### Responsabilités de `XxxCommissionService`

* Décider si une commission doit être générée (règles métier propres au domaine).
* Résoudre le montant via `ICommissionConfigService` (interface du module `commission`).
* Déléguer la création de l'entrée `Commission` à `ICommissionService` (interface publique du module `commission`).

### Ce que le module `commission` ne doit jamais contenir

* Aucune logique propre à un autre domaine (ex : règle spécifique au leasing, à une caution).
* Aucune référence directe aux entités d'un autre module.
* Le module `commission` est un module de persistance et d'accès — pas un module de décision métier.

### Communication inter-modules

```text
XxxCommissionService (module Xxx)
      ↓ ICommissionConfigService   ← résoudre le montant
      ↓ ICommissionService         ← persister la commission
commission/ (module commission)
```

Seules les **interfaces** du module `commission` peuvent être injectées dans d'autres modules.

---

## Règle 4 — Les montants de commission sont configurés en base de données

Tout montant, taux ou seuil de commission **doit être stocké dans la table `commission_config`**, administrable via l'interface utilisateur.

### Interdit

```yaml
commission.leasing.cih: 11
commission.virement.bmci: 33
```

```java
private static final BigDecimal COMMISSION_CIH = new BigDecimal("11");
```

```java
@Value("${commission.leasing.cih}")
private BigDecimal commissionCih;
```

### Obligatoire

* Utiliser `ICommissionConfigService.resolveCommission(banqueNom, organismeNom, type)` pour obtenir le montant.
* La résolution suit l'algorithme à trois niveaux : `SI_MATCH` → `SI_NON_MATCH` → `DEFAULT`.
* Si aucune règle n'existe pour une banque (`hasCommission()` retourne `false`), ne pas générer de commission — ne pas lever d'exception.

### Accès à l'administration

* L'UI de gestion est accessible à `/portail/admin/commission-config`.
* Réservée aux `SYSTEM_ADMIN` (guard frontend + `@PreAuthorize` backend).

---

## Règle 5 — Une opération déjà PAYE n'est jamais supprimable

Toute entité portant un statut `PREVU`/`PAYE` (ou équivalent — `REGLE`, `À payer`/`Payé`, etc.)
**doit bloquer la suppression** dès lors que son statut n'est plus `PREVU`. Cette règle est
transversale à tout le projet, indépendante du module.

**Why:** une opération déjà réglée (PAYE) a déjà généré une transaction bancaire et impacté un
rapprochement — la supprimer casserait la cohérence comptable et l'historique d'audit sans laisser
de trace de la correction. Règle confirmée explicitement par HMAYDA (14/07/2026) : "toute chose payé
ne peut pas être supprimé car c'est déjà réglé, c'est obvious".

### Obligatoire

* Le service métier (`deleteXxx`) doit vérifier `entity.getStatus() != Status.PREVU` (ou
  l'équivalent du module) **avant** l'appel `repository.delete(...)`/`deleteById(...)`, et lever une
  exception de règle métier (`ValidationException`, ou l'exception métier dédiée du module si elle
  existe déjà — ex. `AgiosBusinessException`) sinon.
* Le endpoint `DELETE` correspondant reste réservé `SYSTEM_ADMIN` (`conventions/security.md`) — les
  deux contraintes (rôle + statut) sont indépendantes et cumulatives.
* Côté frontend, le bouton "Supprimer" doit être désactivé (`[disabled]="x.status === Status.PAYE"`)
  en plus d'être conditionné par `isAdmin()` — ne pas se reposer uniquement sur le rejet backend.
* La documentation (`docs/modules/<module>.md`, section Règles métier, et `docs/rbac/<module>.md`,
  colonne Justification de la ligne `DELETE`) doit mentionner explicitement cette restriction.

### Absence de garde constatée dans du code existant

L'absence de cette garde dans un module donné n'est **pas** la preuve d'une exception métier
volontaire — c'est très probablement un oubli (voir l'incident Restitution/Commission du
14/07/2026, où l'absence de garde avait d'abord été interprétée à tort comme un choix assumé). Par
défaut, appliquer la garde ; si un module doit vraiment y déroger, confirmer explicitement avec
HMAYDA avant de le documenter comme une exception.

### Modules déjà audités contre cette règle (14/07/2026)

Impayées, Restitution, Impôts et Taxes, Prélèvements, Chèques, LCN, Virements, Virement
International, Mise à Disposition, Commission, Décompte, Préfinancement, Factoring, Droits
Constatés, Agios (ponctuel), Frais bancaires (guard corrigée en avance de son sweep complet).
Caution suit une règle analogue mais avec un cycle de vie différent (`actif`/archivage, pas
`PREVU`/`PAYE` — suppression bloquée si la caution est archivée). Restent à auditer lors de leur
sweep : Opérations erronées (en cours), États des encours, Rapprochement, Transactions.

---

# Injection des dépendances

* Constructor Injection uniquement.
* Toutes les dépendances sont `private final`.
* Ne jamais utiliser `@Autowired` sur les champs.

---

# Principes de conception

## DRY
## SOLID
## ACID
## KISS
## YAGNI
## Fail Fast
## Idempotence
## Single Source of Truth
## Zéro Dead Code

* Supprimer immédiatement le code inutilisé (`git rm`).
* Ne jamais conserver du code "au cas où".
* Si une suppression est temporairement impossible, documenter le cas dans `TODO.md`.
* Toute valeur configurable doit respecter les conventions de `configuration.md`.