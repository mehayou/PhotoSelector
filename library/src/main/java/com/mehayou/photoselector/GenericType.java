package com.mehayou.photoselector;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class GenericType {

    /**
     * 获取接口泛型Class
     *
     * @param obj      接口对象
     * @param objClass 对象Class
     * @return Class
     */
    static Class<?> findGenericInterfaces(Object obj, Class<?> objClass) {
        if (obj != null && objClass != null) {
            Class<?> aClass = obj.getClass();
            if (objClass.isAssignableFrom(aClass)) {
                Type[] types = obj.getClass().getGenericInterfaces();
                for (Type type : types) {
                    Class<?> genericClass = findGenericClass(type, objClass);
                    if (genericClass != null) {
                        return genericClass;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取泛型Class
     *
     * @param type     泛型
     * @param objClass 对象Class
     * @return Class
     */
    private static Class<?> findGenericClass(Type type, Class objClass) {
        if (type instanceof Class<?>) {
            Type[] types = ((Class<?>) type).getGenericInterfaces();
            if (types.length > 0) {
                for (Type t : types) {
                    Class<?> aClass = findGenericClass(t, objClass);
                    if (aClass != null) {
                        return aClass;
                    }
                }
            }
            return null;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type ownerType = pt.getOwnerType();
            Type rawType = pt.getRawType();
            Type t = pt.getActualTypeArguments()[0];
            if (rawType instanceof Class<?>) {
                Class<?> aClass = (Class<?>) rawType;
                if (aClass.equals(objClass)) {
                    return getTypeClass(t);
                }
            }
            return findGenericClass(t, objClass);
        } else {
            return null;
        }
    }

    /**
     * 获取泛型Class
     *
     * @param type 泛型
     * @return Class
     */
    private static Class<?> getTypeClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            Type innerType = ((ParameterizedType) type).getRawType();
            return (Class<?>) innerType;
        } else if (type instanceof GenericArrayType) {
            Type compType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> typeClass = getTypeClass(compType);
            if (typeClass != null) {
                return Array.newInstance(typeClass, 0).getClass();
            }
        }
        return null;
    }
}
