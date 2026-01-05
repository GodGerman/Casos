package API;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Filtro de autenticacion para proteger endpoints /api/*.
 *
 * Intercepta cada request y valida que exista una sesion con id_usuario.
 * Permite excepciones explicitas para login y preflight CORS.
 *
 */
@WebFilter(urlPatterns = {"/api/*"})
public class AuthFilter implements Filter {

    /**
     * Inicializacion del filtro (sin configuracion adicional).
     * No retorna valor ni genera respuesta; solo cumple el ciclo de vida.
     *
     * Se mantiene vacio porque no se requiere inicializar dependencias.
     *
     *
     * @param filterConfig configuracion del contenedor.
     */
    @Override
    public void init(FilterConfig filterConfig) {
    }

    /**
     * Verifica sesion activa antes de permitir acceso a /api/*.
     * No retorna valor; puede responder 401 si no hay sesion.
     *
     * Se deriva el path relativo al contexto para comparar rutas,
     * omite login/OPTIONS y valida el atributo de sesion "id_usuario".
     *
     *
     * @param request request generico.
     * @param response response generico.
     * @param chain cadena de filtros/servlet destino.
     * @throws IOException si ocurre error de escritura en respuesta.
     * @throws ServletException si falla el flujo del contenedor.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Normaliza la ruta relativa al contexto para comparar endpoints.
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // Permite login y preflight sin sesion.
        if ("/api/auth/login".equals(path) || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("id_usuario") == null) {
            ResponseUtil.writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "sesion_no_iniciada");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Liberacion del filtro (sin recursos a liberar).
     * No retorna valor ni genera respuesta; solo cierra el ciclo de vida.
     *
     * No hay limpieza porque no se crean recursos durante init.
     *
     */
    @Override
    public void destroy() {
    }
}
