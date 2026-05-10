export interface PresignRequest {
  postId: string;
  scene: "knowpost_content" | "knowpost_image";
  contentType?: string;
  ext?: string;
}

export interface PresignResponse {
  objectKey: string;
  putUrl: string;
  headers: Record<string, string>;
  expiresIn: number;
}
