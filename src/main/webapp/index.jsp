<%-- Root index – redirect to movie list (AuthFilter will redirect to login if needed) --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<% response.sendRedirect(request.getContextPath() + "/user/movies"); %>
