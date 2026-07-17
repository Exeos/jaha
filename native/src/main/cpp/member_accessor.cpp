#include "me_exeos_jaha_runtime_MemberAccessor.h"
#include <jni.h>
#include <vector>

jclass c_byte;
jclass c_short;
jclass c_int;
jclass c_long;
jclass c_float;
jclass c_double;
jclass c_boolean;
jclass c_char;

jmethodID m_byteValue;
jmethodID m_shortValue;
jmethodID m_intValue;
jmethodID m_longValue;
jmethodID m_floatValue;
jmethodID m_doubleValue;
jmethodID m_booleanValue;
jmethodID m_charValue;

static std::vector<std::string> parseParamTypes(const std::string &desc) {
    std::vector<std::string> types;
    size_t i = 1;
    while (desc[i] != ')') {
        size_t start = i;
        switch (desc[i]) {
            case 'L':
                while (desc[i] != ';') i++;
                i++;
                break;
            case '[':
                while (desc[i] == '[') i++;
                if (desc[i] == 'L') {
                    while (desc[i] != ';') i++;
                }
                i++;
                break;
            default:
                i++;
                break;
        }

        types.push_back(desc.substr(start, i - start));
    }

    return types;
}

static jvalue unbox(JNIEnv *env, jobject elem, const std::string &type) {
    jvalue v{};

    switch (type[0]) {
        case 'B':
            v.b = env->CallByteMethod(elem, m_byteValue);
            break;
        case 'S':
            v.s = env->CallShortMethod(elem, m_shortValue);
            break;
        case 'I':
            v.i = env->CallIntMethod(elem, m_intValue);
            break;
        case 'J':
            v.j = env->CallLongMethod(elem, m_longValue);
            break;
        case 'F':
            v.f = env->CallFloatMethod(elem, m_floatValue);
            break;
        case 'D':
            v.d = env->CallDoubleMethod(elem, m_doubleValue);
            break;
        case 'Z':
            v.z = env->CallBooleanMethod(elem, m_booleanValue);
            break;
        case 'C':
            v.c = env->CallCharMethod(elem, m_charValue);
            break;
        default:
            v.l = elem;
            break;
    }

    return v;
}

static std::vector<jvalue> unboxParams(JNIEnv *env, jobjectArray argsArray, const char *descriptor) {
    std::vector<std::string> paramTypes = parseParamTypes(descriptor);
    jsize len = env->GetArrayLength(argsArray);

    std::vector<jvalue> args(len);
    for (jsize i = 0; i < len; i++) {
        jobject elem = env->GetObjectArrayElement(argsArray, i);
        args[i] = unbox(env, elem, paramTypes[i]);
    }

    return args;
}

static jclass getClass(JNIEnv *env, jstring name) {
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    jclass clazz = env->FindClass(nameChars);

    env->ReleaseStringUTFChars(name, nameChars);
    return clazz;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_8);

    c_byte = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Byte"));
    c_short = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Short"));
    c_int = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Integer"));
    c_long = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Long"));
    c_float = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Float"));
    c_double = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Double"));
    c_boolean = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Boolean"));
    c_char = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Character"));

    m_byteValue = env->GetMethodID(c_byte, "byteValue", "()B");
    m_shortValue = env->GetMethodID(c_short, "shortValue", "()S");
    m_intValue = env->GetMethodID(c_int, "intValue", "()I");
    m_longValue = env->GetMethodID(c_long, "longValue", "()J");
    m_floatValue = env->GetMethodID(c_float, "floatValue", "()F");
    m_doubleValue = env->GetMethodID(c_double, "doubleValue", "()D");
    m_booleanValue = env->GetMethodID(c_boolean, "booleanValue", "()Z");
    m_charValue = env->GetMethodID(c_char, "charValue", "()C");

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_8);

    env->DeleteGlobalRef(c_byte);
    env->DeleteGlobalRef(c_short);
    env->DeleteGlobalRef(c_int);
    env->DeleteGlobalRef(c_long);
    env->DeleteGlobalRef(c_float);
    env->DeleteGlobalRef(c_double);
    env->DeleteGlobalRef(c_boolean);
    env->DeleteGlobalRef(c_char);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callVoidMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->CallStaticVoidMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars), args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->CallVoidMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);
}

JNIEXPORT jobject JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callObjectMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jobject result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticObjectMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                              args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallObjectMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jbyte JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callByteMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jbyte result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticByteMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                            args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallByteMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jshort JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callShortMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jshort result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticShortMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                             args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallShortMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jint JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callIntegerMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jint result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticIntMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                           args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallIntMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jlong JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callLongMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jlong result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticLongMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                            args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallLongMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jfloat JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callFloatMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jfloat result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticFloatMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                             args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallFloatMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jdouble JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callDoubleMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jdouble result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticDoubleMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                              args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallDoubleMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jboolean JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callBooleanMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jboolean result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticBooleanMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                               args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallBooleanMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars),
                                         args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jchar JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_callCharacterMethod
(JNIEnv *env, jclass, jobject ownerInstance, jobjectArray argsArray, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    std::vector<jvalue> args = unboxParams(env, argsArray, descChars);

    jchar result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->CallStaticCharMethodA(ownerClass, env->GetStaticMethodID(ownerClass, nameChars, descChars),
                                            args.data());
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->CallCharMethodA(ownerInstance, env->GetMethodID(ownerClass, nameChars, descChars), args.data());
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(desc, descChars);

    return result;
}

JNIEXPORT jbyte JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getByteField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jbyte result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticByteField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetByteField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jshort JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getShortField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jshort result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticShortField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetShortField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jint JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getIntegerField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jint result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticIntField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetIntField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jlong JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getLongField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jlong result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticLongField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetLongField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jfloat JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getFloatField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jfloat result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticFloatField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetFloatField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jdouble JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getDoubleField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jdouble result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticDoubleField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetDoubleField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jboolean JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getBooleanField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jboolean result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticBooleanField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetBooleanField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jchar JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getCharacterField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jchar result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticCharField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetCharField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT jobject JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_getObjectField
(JNIEnv *env, jclass, jobject ownerInstance, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    jobject result;
    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        result = env->GetStaticObjectField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars));
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        result = env->GetObjectField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars));
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);

    return result;
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setByteField
(JNIEnv *env, jclass, jobject ownerInstance, jbyte value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticByteField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetByteField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setShortField
(JNIEnv *env, jclass, jobject ownerInstance, jshort value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticShortField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetShortField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setIntegerField
(JNIEnv *env, jclass, jobject ownerInstance, jint value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticIntField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetIntField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setLongField
(JNIEnv *env, jclass, jobject ownerInstance, jlong value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticLongField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetLongField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setFloatField
(JNIEnv *env, jclass, jobject ownerInstance, jfloat value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticFloatField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetFloatField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setDoubleField
(JNIEnv *env, jclass, jobject ownerInstance, jdouble value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticDoubleField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetDoubleField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setBooleanField
(JNIEnv *env, jclass, jobject ownerInstance, jboolean value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticBooleanField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetBooleanField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setCharacterField
(JNIEnv *env, jclass, jobject ownerInstance, jchar value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticCharField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetCharField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}

JNIEXPORT void JNICALL Java_me_exeos_jaha_runtime_MemberAccessor_setObjectField
(JNIEnv *env, jclass, jobject ownerInstance, jobject value, jstring owner, jstring name, jstring desc) {
    jclass ownerClass;
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    const char *descChars = env->GetStringUTFChars(desc, nullptr);

    if (env->IsSameObject(ownerInstance, nullptr)) {
        ownerClass = getClass(env, owner);
        env->SetStaticObjectField(ownerClass, env->GetStaticFieldID(ownerClass, nameChars, descChars), value);
    } else {
        ownerClass = env->GetObjectClass(ownerInstance);
        env->SetObjectField(ownerInstance, env->GetFieldID(ownerClass, nameChars, descChars), value);
    }

    env->DeleteLocalRef(ownerClass);
    env->ReleaseStringUTFChars(desc, descChars);
    env->ReleaseStringUTFChars(name, nameChars);
}