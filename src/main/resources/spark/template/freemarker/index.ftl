<!DOCTYPE html>
<html>
<head>
<#include "header.ftl">
</head>

<body>

<#include "nav.ftl">

<div class="jumbotron text-center">
    <div class="container">
        <a href="/" class="lang-logo">
            <img src="/lang-logo.png">
        </a>
        <h1>Making Neo4j Drivers Great - here lie the photos.</h1>
    </div>
</div>
<div class="container">

    <ul>
    <#list photos as photo>
        <li>
            <form action="/" method="POST" name="votingForm">
                <img src="${photo.uri}"/>
                <Strong>${photo.votes}</Strong>
                <label for="vote">Vote</label>
                <input type="hidden" value="${photo.uri}" name="photo"/>
                <input type="submit" name="Vote" value="Vote"/>
            </form>
        </li>
    </#list>

    </ul>

</div>


</body>
</html>
