import ExpoDynamicAppIconModule from "./ExpoDynamicAppIconModule";
import { DynamicAppIconRegistry } from "./types";

export type IconName = DynamicAppIconRegistry["IconName"];

export async function setAppIcon(
  name: IconName | null,
  isInBackground: boolean = true
): Promise<IconName | "DEFAULT" | false> {
  return ExpoDynamicAppIconModule.setAppIcon(name, isInBackground);
}

export async function getAppIcon(): Promise<IconName | "DEFAULT"> {
  return ExpoDynamicAppIconModule.getAppIcon();
}
