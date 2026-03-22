import { Button, Text, View } from "react-native";

import {
  getAppIcon,
  IconName,
  setAppIcon,
} from "@howincodes/expo-dynamic-app-icon";
import { useState } from "react";

export default function App() {
  const [iconName, setIconName] = useState<IconName | "DEFAULT">();

  return (
    <View
      style={{
        flex: 1,
        backgroundColor: "white",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <View style={{ marginBottom: 16 }}>
        <Button title="get icon!" onPress={async () => setIconName(await getAppIcon())} />
        <Text>{iconName || "Press Button!"}</Text>
      </View>

      <View style={{ marginBottom: 16 }}>
        <Button
          title="change default icon"
          onPress={async () => console.log(await setAppIcon(null))}
        />
      </View>

      <View style={{ marginBottom: 16 }}>
        <Button
          title="change light icon"
          onPress={async () => console.log(await setAppIcon("light", false))}
        />
      </View>

      <Button
        title="change dark icon"
        onPress={async () => console.log(await setAppIcon("dark"))}
      />
    </View>
  );
}
