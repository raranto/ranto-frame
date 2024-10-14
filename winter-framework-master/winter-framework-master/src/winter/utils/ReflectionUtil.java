package winter.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import winter.annotations.RequestParam;
import winter.data.Mapping;
import winter.data.Session;
import winter.exceptions.AnnotationNotFoundException;

public class ReflectionUtil extends Utility {
    public static Object invokeControllerMethod(Mapping mapping, HttpServletRequest req)
            throws AnnotationNotFoundException, ReflectiveOperationException {
        String className = mapping.getClassName();
        String methodName = mapping.getMethodName();

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, mapping.getMethodParamTypes());
            Object[] args = initializeMethodArguments(method.getParameters(), req);

            // Inject session if it's defined
            Object instanceObject = clazz.getDeclaredConstructor().newInstance();
            injectSession(instanceObject, req.getSession());

            // Invoke the controller method
            return method.invoke(instanceObject, args);
        } catch (ClassNotFoundException e) {
            String message = "Class not found: " + className;
            throw new ReflectiveOperationException(message, e);
        } catch (NoSuchMethodException e) {
            String message = "Method not found: " + methodName;
            throw new ReflectiveOperationException(message, e);
        } catch (AnnotationNotFoundException e) {
            throw e;
        } catch (ReflectiveOperationException | NumberFormatException e) {
            String message = "Error invoking method: " + methodName;
            throw new ReflectiveOperationException(message, e);
        }
    }

    private static void injectSession(Object object, HttpSession httpSession)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException,
            IllegalArgumentException, SecurityException {
        Class<?> clazz = object.getClass();
        String attrName = hasSession(clazz);

        if (attrName != null) {
            Method sessionSetterMethod = clazz.getDeclaredMethod(getSetterName(attrName), Session.class);

            // Creates the framework session abstraction object
            Session winterSession = new Session(httpSession);
            sessionSetterMethod.invoke(object, winterSession);
        }
    }

    private static String hasSession(Class<?> clazz) {
        Field[] attributes = clazz.getDeclaredFields();

        for (Field attribute : attributes) {
            if (attribute.getType() == Session.class) {
                return attribute.getName();
            }
        }

        return null;
    }

    private static Object[] initializeMethodArguments(Parameter[] methodParams, HttpServletRequest req)
            throws AnnotationNotFoundException, ReflectiveOperationException {
        List<Object> args = new ArrayList<>();

        for (Parameter param : methodParams) {
            String paramName = getParameterName(param);
            Class<?> paramType = param.getType();
            Object paramValue = createParameterInstance(paramType, paramName, req);
            args.add(paramValue);
        }

        return args.toArray();
    }

    private static String getParameterName(Parameter param) throws AnnotationNotFoundException {
        if (param.isAnnotationPresent(RequestParam.class)) {
            return param.getAnnotation(RequestParam.class).name();
        } else {
            throw new AnnotationNotFoundException(
                    "ETU002539: @RequestMapping annotation not found for " + param.getName());
        }
    }

    private static Object createParameterInstance(Class<?> paramType, String paramName, HttpServletRequest req)
            throws ReflectiveOperationException {
        Object paramValue = paramType.getDeclaredConstructor().newInstance();
        String[] paramNames = getObjectParameters(paramName, req.getParameterNames());
        String[] attrNames = getAttributeNames(paramNames);
        String[] attrValues = getAttributeValues(paramNames, req);
        setObjectAttributes(paramType, paramValue, attrNames, attrValues);
        return paramValue;
    }

    private static void setObjectAttributes(Class<?> classType, Object instance, String[] attrNames,
            String[] attrValues)
            throws ReflectiveOperationException {
        for (int i = 0; i < attrNames.length; i++) {
            String attrName = attrNames[i];
            String attrValue = attrValues[i];
            String setterName = getSetterName(attrName);

            try {
                Class<?> clazz = classType.getDeclaredField(attrName).getType();
                Method setter = classType.getDeclaredMethod(setterName, clazz);
                Object value = convertAttributeValue(attrValue, clazz);
                setter.invoke(instance, value);
            } catch (ReflectiveOperationException | NumberFormatException e) {
                String message = "Error setting attribute: " + attrName;
                throw new ReflectiveOperationException(message, e);
            }
        }
    }

    private static Object convertAttributeValue(String attrValue, Class<?> clazz) {
        if (attrValue == null) {
            return null;
        } else if (clazz == int.class || clazz == Integer.class) {
            return Integer.parseInt(attrValue);
        } else if (clazz == double.class || clazz == Double.class) {
            return Double.parseDouble(attrValue);
        } else if (clazz == float.class || clazz == Float.class) {
            return Float.parseFloat(attrValue);
        } else {
            return attrValue;
        }
    }

    protected static String getSetterName(String attrName) {
        return "set" + Character.toUpperCase(attrName.charAt(0)) + attrName.substring(1);
    }

    private static String[] getAttributeValues(String[] paramNames, HttpServletRequest req) {
        List<String> attributeValues = new ArrayList<>();

        for (String paramName : paramNames) {
            attributeValues.add(req.getParameter(paramName));
        }

        return attributeValues.toArray(new String[0]);
    }

    private static String[] getAttributeNames(String[] objParamNames) {
        List<String> attributeNames = new ArrayList<>();

        for (String paramName : objParamNames) {
            attributeNames.add(paramName.split("\\.")[1]);
        }

        return attributeNames.toArray(new String[0]);
    }

    private static String[] getObjectParameters(String objName, Enumeration<String> paramNames) {
        List<String> objParamNames = new ArrayList<>();

        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();

            if (paramName.matches(objName + ".*")) {
                objParamNames.add(paramName);
            }
        }

        return objParamNames.toArray(new String[0]);
    }
}
