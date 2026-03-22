import ExpoModulesCore

public class ExpoDynamicAppIconModule: Module {
    public func definition() -> ModuleDefinition {
        Name("ExpoDynamicAppIcon")

        AsyncFunction("setAppIcon") { (name: String?, isInBackground: Bool, promise: Promise) in
            self.setAppIcon(name, isInBackground) { success in
                // [I6] Ensure promise resolution happens on main thread
                DispatchQueue.main.async {
                    if success {
                        promise.resolve(name ?? "DEFAULT")
                    } else {
                        promise.resolve(false)
                    }
                }
            }
        }

        // [C3] Use AsyncFunction to safely access UIApplication on the main thread
        AsyncFunction("getAppIcon") { (promise: Promise) in
            DispatchQueue.main.async {
                let iconName = UIApplication.shared.alternateIconName
                let result = iconName?.replacingOccurrences(of: "AppIcon-", with: "") ?? "DEFAULT"
                promise.resolve(result)
            }
        }
    }

    private func setAppIcon(_ iconName: String?, _ isInBackground: Bool = true, completion: @escaping (Bool) -> Void) {
        // Check supportsAlternateIcons on main thread
        DispatchQueue.main.async {
            guard UIApplication.shared.supportsAlternateIcons else {
                completion(false)
                return
            }

            var iconNameToUse: String? = nil
            if let iconName = iconName, !iconName.isEmpty {
                iconNameToUse = "AppIcon-\(iconName)"
            }

            if isInBackground {
                typealias SetAlternateIconName = @convention(c) (NSObject, Selector, NSString?, @escaping (NSError?) -> ()) -> ()

                let selectorString = "_setAlternateIconName:completionHandler:"
                let selector = NSSelectorFromString(selectorString)

                if let methodIMP = UIApplication.shared.method(for: selector) {
                    let method = unsafeBitCast(methodIMP, to: SetAlternateIconName.self)
                    method(UIApplication.shared, selector, iconNameToUse as NSString?) { error in
                        // [I6] Private API completion may fire on arbitrary thread — dispatch to main
                        DispatchQueue.main.async {
                            if let error = error {
                                print("Failed to set app icon (background): \(error.localizedDescription)")
                                completion(false)
                            } else {
                                completion(true)
                            }
                        }
                    }
                } else {
                    // Fallback to public API if private API not available
                    UIApplication.shared.setAlternateIconName(iconNameToUse) { error in
                        if let error = error {
                            print("Failed to set app icon (fallback): \(error.localizedDescription)")
                            completion(false)
                        } else {
                            completion(true)
                        }
                    }
                }
            } else {
                UIApplication.shared.setAlternateIconName(iconNameToUse) { error in
                    if let error = error {
                        print("Failed to set app icon: \(error.localizedDescription)")
                        completion(false)
                    } else {
                        completion(true)
                    }
                }
            }
        }
    }
}
