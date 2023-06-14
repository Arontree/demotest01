$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 获取标题和内容
	//用于获取表单中具有指定 ID 的元素的值。它通常用于获取用户输入的值，例如输入框中的文本或下拉列表中的选项。
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)

     //$.post 是 jQuery 中的一个 AJAX 方法，用于向服务器发送 POST 请求并获取响应数据。
	//url：要请求的 URL。
	//data：要发送到服务器的数据，可以是对象或字符串。
	//success：请求成功时执行的回调函数。
	//dataType：响应数据的类型，可以是 "xml"、"json"、"script" 或 "html"。
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		function(data) {
			data = $.parseJSON(data);
			// 在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	);
}