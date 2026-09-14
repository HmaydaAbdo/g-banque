export interface UserDepartmentRoleResponse {
  id: number;
  departmentId: number;
  departmentName: string;
  roleInDept: 'MEMBER' | 'DIRECTOR';
}
