package main.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.modelView.ModelView;

public class ControllerUtil {
    public static List<String> scanControllers(String packageName) throws IOException, ClassNotFoundException {
        List<String> controllerNames = new ArrayList<>();

        // Convertir le nom du package en chemin de répertoire
        String packagePath = packageName.replace('.', '/');

        // Obtenir le ClassLoader pour charger les ressources du package
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Obtenir toutes les ressources (fichiers de classe) du package
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                // Convertir l'URL en un chemin de fichier
                String filePath = resource.getFile();
                // Récupérer le chemin du package sur le système de fichiers
                File packageDir = new File(filePath);

                // Récupérer la liste des fichiers dans le répertoire du package
                String[] classNames = packageDir.list();
                if (classNames != null) {
                    for (String className : classNames) {
                        // Récupérer le nom de classe (enlever l'extension .class)
                        String fullClassName = packageName + "." + className.replace(".class", "");
                        // Charger la classe par son nom
                        Class<?> clazz = Class.forName(fullClassName);
                        // Vérifier si la classe est annotée avec AnnotationController
                        if (clazz.isAnnotationPresent(Controller.class)) {
                            // Ajouter le nom de classe à la liste des contrôleurs
                            controllerNames.add(clazz.getSimpleName());
                        }
                    }
                }
            }
        }

        return controllerNames;
    }
    
    // Méthode pour extraire le chemin de la ressource demandée à partir de l'URL
    public static String extractRequestedPath(String url, String projectName) {
        // Trouver la position du nom du projet dans l'URL
        int index = url.indexOf(projectName);
        if (index != -1) {
            // Retourner la partie après le nom du projet
            return url.substring(index + projectName.length());
        } else {
            // Si le nom du projet n'est pas trouvé, retourner une chaîne vide ou null selon vos besoins
            return ""; // ou return null;
        }
    }
    
    public static String extractProjectName(String url) {
        // Séparer l'URL en parties en utilisant le délimiteur "/"
        String[] parts = url.split("/");

        // Si l'URL a au moins 2 parties (localhost:8080/CustomProject/...)
        if (parts.length >= 2) {
            // Le deuxième élément est le nom du projet
            return parts[1];
        } else {
            // Si l'URL n'a pas assez de parties, retourner une chaîne vide ou null selon vos besoins
            return ""; // ou return null;
        }
    }
    
    public static HashMap<String, Map<String, String>> getAllMethods(String scanPackage) throws Exception {
        HashMap<String, Map<String, String>> hashMap = new HashMap<>();

        try {
            // Obtenez les noms des contrôleurs dans le package spécifié
            List<String> controllerNames = ControllerUtil.scanControllers(scanPackage);

            for (String controllerName : controllerNames) {
                String qualifiedClassName = scanPackage + "." + controllerName;
                Class<?> controllerClass = Class.forName(qualifiedClassName);
                Map<String, String> map = new HashMap<>();

                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GET.class)) {
                        GET getAnnotation = method.getAnnotation(GET.class);
                        String url = getAnnotation.value();
                        map.put(controllerName, method.getName());

                        // Ajoutez ou mettez à jour l'entrée dans hashMap
                        if (hashMap.containsKey(url)) {
                            hashMap.get(url).put(controllerName, method.getName());
                        } else {
                            Map<String, String> newMap = new HashMap<>();
                            newMap.put(controllerName, method.getName());
                            hashMap.put(url, newMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to scan controllers, " + e);
        }

        return hashMap;
    }
    
    public static void handleRequest(HashMap<String, Map<String, String>> hashMap, String scanPackage, String requestURI, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        boolean methodFound = false;

        for (Map.Entry<String, Map<String, String>> entry : hashMap.entrySet()) {
            String url = entry.getKey();
            Map<String, String> mapx = entry.getValue();
            if (url.equals(requestURI)) {
                for (Map.Entry<String, String> innerEntry : mapx.entrySet()) {
                    String controllerNamex = innerEntry.getKey();
                    String methodName = innerEntry.getValue();
                    String fullClass = scanPackage + "." + controllerNamex;
                    invokeMethod(fullClass, methodName, request, response, out);
                    methodFound = true;
                    break;
                }
                if (methodFound) {
                    break;
                }
            }
        }

        if (!methodFound) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("404 Not Found");
        }

        out.close();
    }

    private static void invokeMethod(String controllerName, String methodName, HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            Class<?> controllerClass = Class.forName(controllerName);
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            Method method;
            boolean hasParams;

            try {
                method = controllerClass.getMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
                hasParams = true;
            } catch (NoSuchMethodException e) {
                method = controllerClass.getMethod(methodName);
                hasParams = false;
            }

            Object result;
            if (hasParams) {
                result = method.invoke(controllerInstance, request, response);
            } else {
                result = method.invoke(controllerInstance);
            }

            if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                for (String key : modelView.getData().keySet()) {
                    request.setAttribute(key, modelView.getData().get(key));
                }

                String url = modelView.getUrl();
                RequestDispatcher dispatcher = request.getRequestDispatcher(url);
                dispatcher.forward(request, response);
            } else if (result instanceof String) {
                out.println((String) result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("Exception: " + e.getMessage());
        }
    }

}
