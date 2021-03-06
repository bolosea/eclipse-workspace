$(function () {
    $("#btnSearch").click(search);
    $("#keyword").click(function (e) {
        $("#keyword").select();
    });

    $("#about").click(function (e) {
       layer.msg("自由 民主。")
    });
    $("#keyword").keydown(function (e) {
        if (e.keyCode == 13) {
            search();
        }
    });
    $("#keyword").autocomplete({
        minLength: 0,
        source: function( request, response ) {
            $.ajax({
                url : "./novel/searchLikeByKey",
                type : "post",
                dataType : "json",
                data : {"keyword":$("#keyword").val()},
                success: function( data ) {
                    //console.log(data);
                    if(data.code==0){
                        response( $.map( data.data, function( item ) {
                            return {
                                value: item
                            }
                        }));
                    }

                }
            });
        },
        messages: {
            noResults: '',
            results: function() {}
        },
        select: function( event, ui ) {
            searchByKeyword(ui.item.label,Page.config.currentPage,Page.config.pageSize);
        }
    });

    $("#nextPage").click(function () {
        var nextPage = $("#nextPage").val();
        var pageSize = $("#nextPage").attr("pageSize");
        var keyword = $("#nextPage").attr("keyword");
        searchByKeyword(keyword,nextPage,pageSize);
    });

    $("#previousPage").click(function () {
        var prevPage = $("#previousPage").val();
        var pageSize = $("#previousPage").attr("pageSize");
        var keyword = $("#previousPage").attr("keyword");
        searchByKeyword(keyword,prevPage,pageSize);
    });

    $("#lastPage").click(function () {
        var lastPage = $("#lastPage").val();
        var pageSize = $("#lastPage").attr("pageSize");
        var keyword = $("#lastPage").attr("keyword");
        searchByKeyword(keyword,lastPage,pageSize);
    });

    $("#gotoPage").click(function () {
        var gotopage_text =  $("#gotopage_text").val().trim();
        if (!gotopage_text) {
            layer.alert("请输入页码",{icon:0});
            return;
        }

        var pageSize = $("#gotoPage").attr("pageSize");
        var keyword = $("#gotoPage").attr("keyword");
        searchByKeyword(keyword,gotopage_text,pageSize);
    });

    $("#firstPage").click(function () {
        var keyword = $("#firstPage").attr("keyword");
        searchByKeyword(keyword,1,10);
    });

    /*头部下拉框移入移出*/
    $(document).on("mouseenter",".header-bar-nav",function(){
        $(this).addClass("open");
    });
    $(document).on("mouseleave",".header-bar-nav",function(){
        $(this).removeClass("open");
    });

});

function search() {
    var currentPage = Page.config.currentPage,pageSize =Page.config.pageSize;
    var keyword = $("#keyword").val().trim();
    if (!keyword) {
        layer.alert("请输入小说名",{icon:0});
        return;
    }
    if (keyword.indexOf("www") == 0) {
        keyword = "http://" + keyword;	//协议补全
    }
    if (keyword.indexOf("http") == 0) {
        searchByUrl(keyword);
    } else {
        searchByKeyword(keyword,currentPage,pageSize);
    }
}

function searchByUrl(url) {
    window.open("./chapterList?base64Url=" + $.base64.encode(url))
}

function searchByKeyword(keyword,currentPage,pageSize) {
    $.ajax({
        url: "./novel/searchByPage",
        type: "post",
        dataType: "json",
        data: {
            "keyword": keyword,
            "currentPage":currentPage,
            "pageSize":pageSize
        },
        error: function (data) {
            console.log(data);
        },
        success: function (data) {
            if (data.code == 0) {
                console.log(data);
                var $novels = data.data.pages;
                if($novels&&$novels.length>0){
                    $("#list").html("");
                    $.each($novels, function (index, novel) {
                        $("#list").append(createNovelTr(index, novel));
                    });
                    $("#page").show();
                    Page.config.currentPage = data.data.currentPage;
                    Page.config.totalPage = data.data.totalPage;
                    Page.render.renderPage(keyword);
                }else{
                    layer.alert("没有查到对应的小说!",{icon:0});
                }

            } else if (data.code == 1) {
                console.log(data.desc);
            } else {
                console.log(data);
            }
        }
    });

    function createNovelTr(index, novel) {
        var trHtml = '<tr>'
            + '		<td>' + (index + 1) + '</td>'
            + '		<td><a href="./novel/getChapters?base64Url=' + $.base64.encode(novel.url) + '" target="_blank">' + novel.name + '</a></td>'
            + '		<td>' + novel.author + '</td>'
            + '		<td><a href="./novel/getChapterDetail?url=' + $.base64.encode(novel.lastUpdateChapterUrl) + '&chapterBase64Url='+$.base64.encode(novel.url)+'" target="_blank">' + novel.lastUpdateChapter + '</a></td>'
            + '		<td>' + ((novel.status == 1) ? "连载" : "完结") + '</td>'
            + '	    <td>' + getPlatform(novel.platformId) + '</td>'
            + '	    <td><a href="./novel/download?base64Url=' + $.base64.encode(novel.url)+"&name="+novel.name + '" target="_blank">' + "下载" + '</a></td>'
            + '</tr>';
        return $(trHtml);
    }

    function getPlatform(platformId) {
        if (platformId == 1) {
            return "顶点小说";
        } else if (platformId == 5) {
            return "58小说";
        } else if (platformId == 4) {
            return "看书中";
        } else if (platformId == 3) {
            return "笔下文学";
        } else {
            return "为知";
        }
    }

}