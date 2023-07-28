$(function(){
    $("#uploadForm").submit(upload);
});
// 在这上传文件到云服务器上
function upload() {
    // 异步请求
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false,
        contentType: false,
        // data：指定了要上传的数据，这里使用了 FormData 对象，通过 new FormData($("#uploadForm")[0]) 创建，将表单中的数据包装成FormData对象。
        data: new FormData($("#uploadForm")[0]),
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
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
    // 表示到此为止了
    return false;
}