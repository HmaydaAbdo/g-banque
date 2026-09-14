  import { PorteeNature } from './PorteeNature';

  export interface NatureLigneFonctionnelleResponse {
    id: number;
    code: string;
    label: string;
    description: string;
    portee: PorteeNature;
    dateCreation: string;
    dateModification: string;
  }
