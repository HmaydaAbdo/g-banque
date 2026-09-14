export interface RapprochementActifsDto {
  // EN_COURS — always present
  enCoursId: number;
  enCoursPeriode: string;          // "YYYY-MM"
  enCoursSoldeDebut: number;
  enCoursSoldeFin: number;
  enCoursDebit: number;
  enCoursCredit: number;
  enCoursNbTransactions: number;

  // CLOTURE_PROVISOIRE — null when absent
  provisoireId: number | null;
  provisoirePeriode: string | null;
  provisoireSoldeDebut: number | null;
  provisoireSoldeFin: number | null;
  provisoireDebit: number | null;
  provisoireCredit: number | null;
  provisoireNbTransactions: number;
}
