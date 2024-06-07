# Framework_ITU BY FANDRESENA

Configuration web.xml:
    -Ajouter votre package dans servlet 
    -Exemple:
    <servlet>
    <servlet-name>framework</servlet-name>
    <servlet-class>main.controller.FrontController</servlet-class>
      <init-param>
          <param-name>scanPackage</param-name>
          <param-value>main.emp</param-value>
      </init-param>
