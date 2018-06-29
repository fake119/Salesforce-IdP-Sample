<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>acs</title>
</head>
<body>
=== 하기의 정보를 Salesforce로 부터 수신하였음 === <br />
Assert Result : ${assert_result} <br/><br/>
userId : ${userId} <br/>
username : ${username} <br/>
federationId : ${federationId} <br/>
userFullName : ${userFullName} <br/>
email : ${email} <br/>
RelayState : ${RelayState} <br/><br/>
<a href="/partner/menu1/">원래 Click했던 menu1로 이동</a> <br/>
<a href="https://ksisso-dev-ed.my.salesforce.com/secur/logout.jsp">salesforce logout(App logout은 https(SSL)만 가능)</a>
</body>
</html>