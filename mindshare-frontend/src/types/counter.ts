export type EntityType = "knowpost";

export interface CounterData {
  like: number;
  fav: number;
}

export interface ActionRequest {
  entityType: EntityType;
  entityId: string;
}
