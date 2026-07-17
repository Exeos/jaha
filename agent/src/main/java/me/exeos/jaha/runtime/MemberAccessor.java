package me.exeos.jaha.runtime;

/**
 * Provides methods for bypassing access limitation.
 * This is required because we copy the original, unhooked methods into new classes.
 * This means access to private members now come from another class.
 */
public class MemberAccessor {

    static {
        NativeLoader.ensureLoaded();
    }

    public static native byte getByteField(Object ownerInstance, String owner, String name, String desc);

    public static native short getShortField(Object ownerInstance, String owner, String name, String desc);

    public static native int getIntegerField(Object ownerInstance, String owner, String name, String desc);

    public static native long getLongField(Object ownerInstance, String owner, String name, String desc);

    public static native float getFloatField(Object ownerInstance, String owner, String name, String desc);

    public static native double getDoubleField(Object ownerInstance, String owner, String name, String desc);

    public static native boolean getBooleanField(Object ownerInstance, String owner, String name, String desc);

    public static native char getCharacterField(Object ownerInstance, String owner, String name, String desc);

    public static native Object getObjectField(Object ownerInstance, String owner, String name, String desc);

    public static native void setByteField(Object ownerInstance, byte value, String owner, String name, String desc);

    public static native void setShortField(Object ownerInstance, short value, String owner, String name, String desc);

    public static native void setIntegerField(Object ownerInstance, int value, String owner, String name, String desc);

    public static native void setLongField(Object ownerInstance, long value, String owner, String name, String desc);

    public static native void setFloatField(Object ownerInstance, float value, String owner, String name, String desc);

    public static native void setDoubleField(Object ownerInstance, double value, String owner, String name, String desc);

    public static native void setBooleanField(Object ownerInstance, boolean value, String owner, String name, String desc);

    public static native void setCharacterField(Object ownerInstance, char value, String owner, String name, String desc);

    public static native void setObjectField(Object ownerInstance, Object value, String owner, String name, String desc);

    public static native void callVoidMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native Object callObjectMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native byte callByteMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native short callShortMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native int callIntegerMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native long callLongMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native float callFloatMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native double callDoubleMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native boolean callBooleanMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native char callCharacterMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

}
