$(function(){
    $("form").submit(check_data);
    $("input").focus(clear_error);
    $("#uploadForm").submit(upload);
});

function check_data() {
    var pwd1 = $("#new-password").val();
    var pwd2 = $("#confirm-password").val();
    if(pwd1 != pwd2) {
        $("#confirm-password").addClass("is-invalid");
        return false;
    }
    return true;
}

function clear_error() {
    $(this).removeClass("is-invalid");
}

function upload() {
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false, //不要把表单的数据转换成字符串提交
        contentType: false, //不让jquery去设置上传的类型
        data: new FormData($("#uploadForm")[0]),
        success: function (data) {
            if (data && data.code == 0) {
                //发送Ajax请求前,将CSRF令牌设置到请求的消息头中
                var token = $("meta[name='_csrf']").attr("content");
                var header = $("meta[name='_csrf_header']").attr("content");
                $(document).ajaxSend(function (e, xhr, options) {
                    xhr.setRequestHeader(header, token);
                });
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"filename":$("input[name='key']").val()},
                    function (data) {
                        data = $.parseJSON(data);
                        if (data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    
    return false;
}