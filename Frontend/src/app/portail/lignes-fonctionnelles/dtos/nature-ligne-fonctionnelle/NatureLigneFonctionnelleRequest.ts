import { PorteeNature } from './PorteeNature';

export interface NatureLigneFonctionnelleRequest {
  code: string;
  label: string;
  description: string;
  portee: PorteeNature;
}
