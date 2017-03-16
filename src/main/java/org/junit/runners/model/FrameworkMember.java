package org.junit.runners.model;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Parent class for {@link FrameworkField} and {@link FrameworkMethod}
 *
 * @since 4.7
 */
public abstract class FrameworkMember<T extends FrameworkMember<T>> implements
        Annotatable {
    /**
     * 判断当前方法是否已添加到某个方法集合中
     * @param otherMember
     * @return
     */
    abstract boolean isShadowedBy(T otherMember);

    boolean isShadowedBy(List<T> members) {
        for (T each : members) {
            if (isShadowedBy(each)) {
                return true;
            }
        }
        return false;
    }

    protected abstract int getModifiers();

    /**
     * Returns true if this member is static, false if not.
     */
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    /**
     * Returns true if this member is public, false if not.
     */
    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public abstract String getName();

    public abstract Class<?> getType();

    public abstract Class<?> getDeclaringClass();
}
