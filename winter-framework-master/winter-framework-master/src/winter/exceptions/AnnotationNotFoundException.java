package winter.exceptions;

public class AnnotationNotFoundException extends Exception {
    public AnnotationNotFoundException(String msg) {
        super(msg);
    }

    public AnnotationNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
