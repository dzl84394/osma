$(function() {
    var rangesConf = {};
        rangesConf[I18n.daterangepicker_ranges_recent_hour] = [moment().subtract(1, 'hours'), moment()];
        rangesConf[I18n.daterangepicker_ranges_today] = [moment().startOf('day'), moment().endOf('day')];
        rangesConf[I18n.daterangepicker_ranges_yesterday] = [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')];
        rangesConf[I18n.daterangepicker_ranges_this_month] = [moment().startOf('month'), moment().endOf('month')];
        rangesConf[I18n.daterangepicker_ranges_last_month] = [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')];
        rangesConf[I18n.daterangepicker_ranges_recent_week] = [moment().subtract(1, 'weeks').startOf('day'), moment().endOf('day')];
        rangesConf[I18n.daterangepicker_ranges_recent_month] = [moment().subtract(1, 'months').startOf('day'), moment().endOf('day')];


    $('#filterTime').daterangepicker({
        autoApply:false,
        singleDatePicker:false,
        showDropdowns:false,        // 是否显示年月选择条件
		timePicker: true, 			// 是否显示小时和分钟选择条件
		timePickerIncrement: 10, 	// 时间的增量，单位为分钟
        timePicker24Hour : true,
        opens : 'left', //日期选择框的弹出位置
		ranges: rangesConf,
        locale : {
            format: 'YYYY-MM-DD HH:mm:ss',
            separator : ' - ',
            customRangeLabel : I18n.daterangepicker_custom_name ,
            applyLabel : I18n.system_ok ,
            cancelLabel : I18n.system_cancel ,
            fromLabel : I18n.daterangepicker_custom_starttime ,
            toLabel : I18n.daterangepicker_custom_endtime ,
            daysOfWeek : I18n.daterangepicker_custom_daysofweek.split(',') ,        // '日', '一', '二', '三', '四', '五', '六'
            monthNames : I18n.daterangepicker_custom_monthnames.split(',') ,        // '一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'
            firstDay : 1
        },
        startDate: rangesConf[I18n.daterangepicker_ranges_today][0],
        endDate: rangesConf[I18n.daterangepicker_ranges_today][1]
	});

	// init date tables
	var jobTable = $("#job_list").dataTable({
		"deferRender": true,
		"processing" : true,
	    "serverSide": true,
		"ajax": {
			url: base_url + "/jobinfoCheck/checkList",
			type:"post",
	        data : function ( d ) {
	        	var obj = {};
	        	obj.jobGroup = $('#jobGroup').val();
                obj.triggerStatus = $('#triggerStatus').val();
                obj.jobDesc = $('#jobDesc').val();
                obj.executorHandler = $('#executorHandler').val();
                obj.author = $('#author').val();
                obj.start = d.start;
                obj.length = d.length;
                obj.filterTime = $('#filterTime').val();
                return obj;
            }
	    },
	    "searching": false,
	    "ordering": false,
	    //"scrollX": true,	// scroll x，close self-adaption
	    "columns": [
	                {
	                	"data": 'id',
						"bSortable": false,
						"visible" : true,
						"width":'7%'
					},
	                {
	                	"data": 'jobGroup',
	                	"visible" : false,
	                	"render": function ( data, type, row ) {
	            			var groupMenu = $("#jobGroup").find("option");
	            			for ( var index in $("#jobGroup").find("option")) {
	            				if ($(groupMenu[index]).attr('value') == data) {
									return $(groupMenu[index]).html();
								}
							}
	            			return data;
	            		}
            		},
	                {
	                	"data": 'jobDesc',
						"visible" : true,
						"width":'25%'
					},
					{
						"data": 'scheduleType',
						"visible" : true,
						"width":'13%',
						"render": function ( data, type, row ) {
							if (row.scheduleConf) {
								return row.scheduleType + '：'+ row.scheduleConf;
							} else {
								return row.scheduleType;
							}
						}
					},
					{
						"data": 'glueType',
						"width":'15%',
						"visible" : true,
						"render": function ( data, type, row ) {
							var glueTypeTitle = findGlueTypeTitle(row.glueType);
                            if (row.executorHandler) {
                                return glueTypeTitle +"：" + row.executorHandler;
                            } else {
                                return glueTypeTitle;
                            }
						}
					},
	                { "data": 'executorParam', "visible" : false},
	                {
	                	"data": 'addTime',
	                	"visible" : false,
	                	"render": function ( data, type, row ) {
	                		return data?moment(new Date(data)).format("YYYY-MM-DD HH:mm:ss"):"";
	                	}
	                },
	                {
	                	"data": 'updateTime',
	                	"visible" : false,
	                	"render": function ( data, type, row ) {
	                		return data?moment(new Date(data)).format("YYYY-MM-DD HH:mm:ss"):"";
	                	}
	                },
	                {
                        "data": 'triggerNextTime',
                        "visible" : true,
                        "width":'15%',
                        "render": function ( data, type, row ) {
                            return data?moment(new Date(data)).format("YYYY-MM-DD HH:mm:ss"):"";
                        }
                    },
	                { "data": 'author', "visible" : true, "width":'10%'},
	                { "data": 'alarmEmail', "visible" : false},
	                {
	                	"data": 'triggerStatus',
						"width":'10%',
	                	"visible" : true,
	                	"render": function ( data, type, row ) {
                            // status
                            if (1 == data) {
                                return '<small class="label label-success" >RUNNING</small>';
                            } else {
                                return '<small class="label label-default" >STOP</small>';
                            }
	                		return data;
	                	}
	                }
	            ],
		"language" : {
			"sProcessing" : I18n.dataTable_sProcessing ,
			"sLengthMenu" : I18n.dataTable_sLengthMenu ,
			"sZeroRecords" : I18n.dataTable_sZeroRecords ,
			"sInfo" : I18n.dataTable_sInfo ,
			"sInfoEmpty" : I18n.dataTable_sInfoEmpty ,
			"sInfoFiltered" : I18n.dataTable_sInfoFiltered ,
			"sInfoPostFix" : "",
			"sSearch" : I18n.dataTable_sSearch ,
			"sUrl" : "",
			"sEmptyTable" : I18n.dataTable_sEmptyTable ,
			"sLoadingRecords" : I18n.dataTable_sLoadingRecords ,
			"sInfoThousands" : ",",
			"oPaginate" : {
				"sFirst" : I18n.dataTable_sFirst ,
				"sPrevious" : I18n.dataTable_sPrevious ,
				"sNext" : I18n.dataTable_sNext ,
				"sLast" : I18n.dataTable_sLast
			},
			"oAria" : {
				"sSortAscending" : I18n.dataTable_sSortAscending ,
				"sSortDescending" : I18n.dataTable_sSortDescending
			}
		}
	});

    // table data
    var tableData = {};

	// search btn
	$('#searchBtn').on('click', function(){
		jobTable.fnDraw();
	});

	// jobGroup change
	$('#jobGroup').on('change', function(){
        //reload
        var jobGroup = $('#jobGroup').val();
//        window.location.href = base_url + "/jobinfoCheck?jobGroup=" + jobGroup;
    });

	// job operate
	$("#job_list").on('click', '.job_operate',function() {
		var typeName;
		var url;
		var needFresh = false;

		var type = $(this).attr("_type");
		if ("job_pause" == type) {
			typeName = I18n.jobinfo_opt_stop ;
			url = base_url + "/jobinfo/stop";
			needFresh = true;
		} else if ("job_resume" == type) {
			typeName = I18n.jobinfo_opt_start ;
			url = base_url + "/jobinfo/start";
			needFresh = true;
		} else if ("job_del" == type) {
			typeName = I18n.system_opt_del ;
			url = base_url + "/jobinfo/remove";
			needFresh = true;
		} else {
			return;
		}

		var id = $(this).parents('ul').attr("_id");

		layer.confirm( I18n.system_ok + typeName + '?', {
			icon: 3,
			title: I18n.system_tips ,
            btn: [ I18n.system_ok, I18n.system_cancel ]
		}, function(index){
			layer.close(index);

			$.ajax({
				type : 'POST',
				url : url,
				data : {
					"id" : id
				},
				dataType : "json",
				success : function(data){
					if (data.code == 200) {
                        layer.msg( typeName + I18n.system_success );
                        if (needFresh) {
                            //window.location.reload();
                            jobTable.fnDraw(false);
                        }
					} else {
                        layer.msg( data.msg || typeName + I18n.system_fail );
					}
				}
			});
		});
	});

    // job trigger
    $("#job_list").on('click', '.job_trigger',function() {
        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

        $("#jobTriggerModal .form input[name='id']").val( row.id );
        $("#jobTriggerModal .form textarea[name='executorParam']").val( row.executorParam );

        $('#jobTriggerModal').modal({backdrop: false, keyboard: false}).modal('show');
    });
    $("#jobTriggerModal .ok").on('click',function() {
        $.ajax({
            type : 'POST',
            url : base_url + "/jobinfo/trigger",
            data : {
                "id" : $("#jobTriggerModal .form input[name='id']").val(),
                "executorParam" : $("#jobTriggerModal .textarea[name='executorParam']").val(),
				"addressList" : $("#jobTriggerModal .textarea[name='addressList']").val()
            },
            dataType : "json",
            success : function(data){
                if (data.code == 200) {
                    $('#jobTriggerModal').modal('hide');

                    layer.msg( I18n.jobinfo_opt_run + I18n.system_success );
                } else {
                    layer.msg( data.msg || I18n.jobinfo_opt_run + I18n.system_fail );
                }
            }
        });
    });
    $("#jobTriggerModal").on('hide.bs.modal', function () {
        $("#jobTriggerModal .form")[0].reset();
    });


    // job registryinfo
    $("#job_list").on('click', '.job_registryinfo',function() {
        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

        var jobGroup = row.jobGroup;

        $.ajax({
            type : 'POST',
            url : base_url + "/jobgroup/loadById",
            data : {
                "id" : jobGroup
            },
            dataType : "json",
            success : function(data){

                var html = '<div>';
                if (data.code == 200 && data.content.registryList) {
                    for (var index in data.content.registryList) {
                        html += (parseInt(index)+1) + '. <span class="badge bg-green" >' + data.content.registryList[index] + '</span><br>';
                    }
                }
                html += '</div>';

                layer.open({
                    title: I18n.jobinfo_opt_registryinfo ,
                    btn: [ I18n.system_ok ],
                    content: html
                });

            }
        });

    });

    // job_next_time
    $("#job_list").on('click', '.job_next_time',function() {
        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

        $.ajax({
            type : 'POST',
            url : base_url + "/jobinfo/nextTriggerTime",
            data : {
                "scheduleType" : row.scheduleType,
				"scheduleConf" : row.scheduleConf
            },
            dataType : "json",
            success : function(data){

            	if (data.code != 200) {
                    layer.open({
                        title: I18n.jobinfo_opt_next_time ,
                        btn: [ I18n.system_ok ],
                        content: data.msg
                    });
				} else {
                    var html = '<center>';
                    if (data.code == 200 && data.content) {
                        for (var index in data.content) {
                            html += '<span>' + data.content[index] + '</span><br>';
                        }
                    }
                    html += '</center>';

                    layer.open({
                        title: I18n.jobinfo_opt_next_time ,
                        btn: [ I18n.system_ok ],
                        content: html
                    });
				}

            }
        });

    });





	// scheduleType change
	$(".scheduleType").change(function(){
		var scheduleType = $(this).val();
		$(this).parents("form").find(".schedule_conf").hide();
		$(this).parents("form").find(".schedule_conf_" + scheduleType).show();

	});

    // glueType change
    $(".glueType").change(function(){
		// executorHandler
        var $executorHandler = $(this).parents("form").find("input[name='executorHandler']");
        var glueType = $(this).val();
        if ('BEAN' != glueType) {
            $executorHandler.val("");
            $executorHandler.attr("readonly","readonly");
        } else {
            $executorHandler.removeAttr("readonly");
        }
    });

	$("#addModal .glueType").change(function(){
		// glueSource
		var glueType = $(this).val();
		if ('GLUE_GROOVY'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_java").val() );
		} else if ('GLUE_SHELL'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_shell").val() );
		} else if ('GLUE_PYTHON'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_python").val() );
		} else if ('GLUE_PHP'==glueType){
            $("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_php").val() );
        } else if ('GLUE_NODEJS'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_nodejs").val() );
		} else if ('GLUE_POWERSHELL'==glueType){
            $("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_powershell").val() );
        } else {
            $("#addModal .form textarea[name='glueSource']").val("");
		}
	});

	// update
	$("#job_list").on('click', '.update',function() {

        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

		// fill base
		$("#updateModal .form input[name='id']").val( row.id );
		$('#updateModal .form select[name=jobGroup] option[value='+ row.jobGroup +']').prop('selected', true);
		$("#updateModal .form input[name='jobDesc']").val( row.jobDesc );
		$("#updateModal .form input[name='author']").val( row.author );
		$("#updateModal .form input[name='alarmEmail']").val( row.alarmEmail );

		// fill trigger
		$('#updateModal .form select[name=scheduleType] option[value='+ row.scheduleType +']').prop('selected', true);
		$("#updateModal .form input[name='scheduleConf']").val( row.scheduleConf );
		if (row.scheduleType == 'CRON') {
			$("#updateModal .form input[name='schedule_conf_CRON']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_RATE') {
			$("#updateModal .form input[name='schedule_conf_FIX_RATE']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_DELAY') {
			$("#updateModal .form input[name='schedule_conf_FIX_DELAY']").val( row.scheduleConf );
		}

		// 》init scheduleType
		$("#updateModal .form select[name=scheduleType]").change();

		// fill job
		$('#updateModal .form select[name=glueType] option[value='+ row.glueType +']').prop('selected', true);
		$("#updateModal .form input[name='executorHandler']").val( row.executorHandler );
		$("#updateModal .form textarea[name='executorParam']").val( row.executorParam );

		// 》init glueType
		$("#updateModal .form select[name=glueType]").change();

		// 》init-cronGen
		$("#updateModal .form input[name='schedule_conf_CRON']").show().siblings().remove();
		$("#updateModal .form input[name='schedule_conf_CRON']").cronGen({});

		// fill advanced
		$('#updateModal .form select[name=executorRouteStrategy] option[value='+ row.executorRouteStrategy +']').prop('selected', true);
		$("#updateModal .form input[name='childJobId']").val( row.childJobId );
		$('#updateModal .form select[name=misfireStrategy] option[value='+ row.misfireStrategy +']').prop('selected', true);
		$('#updateModal .form select[name=executorBlockStrategy] option[value='+ row.executorBlockStrategy +']').prop('selected', true);
		$("#updateModal .form input[name='executorTimeout']").val( row.executorTimeout );
        $("#updateModal .form input[name='executorFailRetryCount']").val( row.executorFailRetryCount );

		// show
		$('#updateModal').modal({backdrop: false, keyboard: false}).modal('show');
	});
	var updateModalValidate = $("#updateModal .form").validate({
		errorElement : 'span',
        errorClass : 'help-block',
        focusInvalid : true,

		rules : {
			jobDesc : {
				required : true,
				maxlength: 50
			},
			author : {
				required : true
			}
		},
		messages : {
			jobDesc : {
                required : I18n.system_please_input + I18n.jobinfo_field_jobdesc
			},
			author : {
				required : I18n.system_please_input + I18n.jobinfo_field_author
			}
		},
		highlight : function(element) {
            $(element).closest('.form-group').addClass('has-error');
        },
        success : function(label) {
            label.closest('.form-group').removeClass('has-error');
            label.remove();
        },
        errorPlacement : function(error, element) {
            element.parent('div').append(error);
        },
        submitHandler : function(form) {

            // process executorTimeout + executorFailRetryCount
            var executorTimeout = $("#updateModal .form input[name='executorTimeout']").val();
            if(!/^\d+$/.test(executorTimeout)) {
                executorTimeout = 0;
            }
            $("#updateModal .form input[name='executorTimeout']").val(executorTimeout);
            var executorFailRetryCount = $("#updateModal .form input[name='executorFailRetryCount']").val();
            if(!/^\d+$/.test(executorFailRetryCount)) {
                executorFailRetryCount = 0;
            }
            $("#updateModal .form input[name='executorFailRetryCount']").val(executorFailRetryCount);


			// process schedule_conf
			var scheduleType = $("#updateModal .form select[name='scheduleType']").val();
			var scheduleConf;
			if (scheduleType == 'CRON') {
				scheduleConf = $("#updateModal .form input[name='cronGen_display']").val();
			} else if (scheduleType == 'FIX_RATE') {
				scheduleConf = $("#updateModal .form input[name='schedule_conf_FIX_RATE']").val();
			} else if (scheduleType == 'FIX_DELAY') {
				scheduleConf = $("#updateModal .form input[name='schedule_conf_FIX_DELAY']").val();
			}
			$("#updateModal .form input[name='scheduleConf']").val( scheduleConf );

			// post
    		$.post(base_url + "/jobinfo/update", $("#updateModal .form").serialize(), function(data, status) {
    			if (data.code == "200") {
					$('#updateModal').modal('hide');
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: I18n.system_update_suc ,
						icon: '1',
						end: function(layero, index){
							//window.location.reload();
							jobTable.fnDraw();
						}
					});
    			} else {
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: (data.msg || I18n.system_update_fail ),
						icon: '2'
					});
    			}
    		});
		}
	});
	$("#updateModal").on('hide.bs.modal', function () {
        updateModalValidate.resetForm();
        $("#updateModal .form")[0].reset();
        $("#updateModal .form .form-group").removeClass("has-error");
	});

    /**
	 * find title by name, GlueType
     */
	function findGlueTypeTitle(glueType) {
		var glueTypeTitle;
        $("#addModal .form select[name=glueType] option").each(function () {
            var name = $(this).val();
            var title = $(this).text();
            if (glueType == name) {
                glueTypeTitle = title;
                return false
            }
        });
        return glueTypeTitle;
    }

    // job_copy
	$("#job_list").on('click', '.job_copy',function() {

		var id = $(this).parents('ul').attr("_id");
		var row = tableData['key'+id];

		// fill base
		$('#addModal .form select[name=jobGroup] option[value='+ row.jobGroup +']').prop('selected', true);
		$("#addModal .form input[name='jobDesc']").val( row.jobDesc );
		$("#addModal .form input[name='author']").val( row.author );
		$("#addModal .form input[name='alarmEmail']").val( row.alarmEmail );

		// fill trigger
		$('#addModal .form select[name=scheduleType] option[value='+ row.scheduleType +']').prop('selected', true);
		$("#addModal .form input[name='scheduleConf']").val( row.scheduleConf );
		if (row.scheduleType == 'CRON') {
			$("#addModal .form input[name='schedule_conf_CRON']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_RATE') {
			$("#addModal .form input[name='schedule_conf_FIX_RATE']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_DELAY') {
			$("#addModal .form input[name='schedule_conf_FIX_DELAY']").val( row.scheduleConf );
		}

		// 》init scheduleType
		$("#addModal .form select[name=scheduleType]").change();

		// fill job
		$('#addModal .form select[name=glueType] option[value='+ row.glueType +']').prop('selected', true);
		$("#addModal .form input[name='executorHandler']").val( row.executorHandler );
		$("#addModal .form textarea[name='executorParam']").val( row.executorParam );

		// 》init glueType
		$("#addModal .form select[name=glueType]").change();

		// 》init-cronGen
		$("#addModal .form input[name='schedule_conf_CRON']").show().siblings().remove();
		$("#addModal .form input[name='schedule_conf_CRON']").cronGen({});

		// fill advanced
		$('#addModal .form select[name=executorRouteStrategy] option[value='+ row.executorRouteStrategy +']').prop('selected', true);
		$("#addModal .form input[name='childJobId']").val( row.childJobId );
		$('#addModal .form select[name=misfireStrategy] option[value='+ row.misfireStrategy +']').prop('selected', true);
		$('#addModal .form select[name=executorBlockStrategy] option[value='+ row.executorBlockStrategy +']').prop('selected', true);
		$("#addModal .form input[name='executorTimeout']").val( row.executorTimeout );
		$("#addModal .form input[name='executorFailRetryCount']").val( row.executorFailRetryCount );

		// show
		$('#addModal').modal({backdrop: false, keyboard: false}).modal('show');
	});


});
