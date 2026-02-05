<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>Success</title></head>
<body>
  <h1>Welcome!</h1>
  <p>${param.msg}</p>
  <p>Logged in as: ${sessionScope.userPhone}</p>
  <a href="index.html">Back to Home</a>
</body>
</html>