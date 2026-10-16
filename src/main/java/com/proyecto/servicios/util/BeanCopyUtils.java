package com.proyecto.servicios.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * Utileria para copiar propiedades entre objetos de forma dinamica y limpia.
 * Permite reducir decenas de lineas manuales de setters y verificaciones if != null.
 */
public final class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    /**
     * Copia unicamentes los campos no nulos (util para operaciones PATCH).
     */
    public static void copyNonNullProperties(Object source, Object target, String... ignoreProperties) {
        Set<String> emptyNames = getNullPropertyNames(source);
        for (String ignore : ignoreProperties) {
            emptyNames.add(ignore);
        }
        BeanUtils.copyProperties(source, target, emptyNames.toArray(new String[0]));
    }

    private static Set<String> getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames;
    }
}