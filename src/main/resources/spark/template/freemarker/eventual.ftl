<!DOCTYPE html>
<html>
<head>
<#include "header.ftl">
</head>

<body>

<#include "nav.ftl">

<div class="jumbotron text-center">
    <div class="container">
        <img src="http://info.neo4j.com/rs/773-GON-065/images/neo4j_logo.png" width="200px">
        <h1>Eventual Consistency - Vote without bookmarks.</h1>
    </div>
</div>

<div class="container">
<#list photos?chunk(2) as row>
    <div class="row">
        <#list row as photo>
            <div class="col-xs-6">
                <div class="card text-center">
                    <div class="card-block<#if row?is_first &&
                    photo?is_first>bg-success</#if>">
                        <h1 class="card-title">${photo.votes} Votes</h1>
                    </div>
                    <div class="card-block text-xs-center">
                        <form action="/eventual" method="POST" name="votingForm">
                            <input type="hidden" value="${photo.uri}" name="photo"/>
                            <input class="btn btn-primary" type="submit" name="Vote" value="Vote"/>
                        </form>
                    </div>
                    <img class="card-img-top" src="${photo.uri}" height="200px"/>
                </div>
            </div>
        </#list>
    </div>
</#list>
</div>


</body>
</html>
