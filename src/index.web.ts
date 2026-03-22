import { DynamicAppIconRegistry } from "./types";

export type IconName = DynamicAppIconRegistry["IconName"];

export async function setAppIcon(
  name: IconName | null,
  isInBackground: boolean = true
): Promise<IconName | "DEFAULT" | false> {
  console.warn("setAppIcon is not supported on web");
  return false;
}

export async function getAppIcon(): Promise<IconName | "DEFAULT"> {
  console.warn("getAppIcon is not supported on web");
  return "DEFAULT";
}
