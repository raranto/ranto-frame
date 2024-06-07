package main.controller;

import jakarta.servlet.ServletConfig;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.lang.reflect.Method;

public class FrontController extends HttpServlet {
    private String scanPackage;
    private HashMap<String, Map<String, String>> hashMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        scanPackage = config.getInitParameter("scanPackage");
        try {
            hashMap = ControllerUtil.getAllMethods(scanPackage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String projectName = ControllerUtil.extractProjectName(requestURI);
        requestURI = ControllerUtil.extractRequestedPath(requestURI, projectName);
        ControllerUtil.handleRequest(hashMap, scanPackage, requestURI, request, response);
    }

    

    
}
