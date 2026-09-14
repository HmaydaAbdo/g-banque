export interface DepartmentRoleRequest {
  departmentId: number;
  roleInDept: 'MEMBER' | 'DIRECTOR' | null;
}
