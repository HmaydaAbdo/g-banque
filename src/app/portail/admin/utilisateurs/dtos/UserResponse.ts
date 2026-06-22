import { UserDepartmentRoleResponse } from './UserDepartmentRoleResponse';

export interface UserResponse {
  id: number;
  username: string;
  enabled: boolean;
  systemAdmin: boolean;
  departmentRoles: UserDepartmentRoleResponse[];
}
