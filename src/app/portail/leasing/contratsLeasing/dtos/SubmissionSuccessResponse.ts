export interface SubmissionSuccessResponse {
  message: string;
  contractId?: string; // Or whatever ID your backend returns
  // Add other fields if your successful submission response includes them
}
