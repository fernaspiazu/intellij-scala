package org.jetbrains.idea.devkit.scala;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class DevkitBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaDevkitBundle";

    private static final DevkitBundle INSTANCE = new DevkitBundle();

    private DevkitBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
