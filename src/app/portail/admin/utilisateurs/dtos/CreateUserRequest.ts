export interface CreateUserRequest {
  username: string;
  password: string;
  enabled: boolean | null;
}
