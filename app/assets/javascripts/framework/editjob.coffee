$ ->
  check_input = () ->
    name = $('#name').val()
    if (name.length > 0)
      $('#save-button').prop('disabled', false)
    else
      $('#save-button').prop('disabled', true)

  $('#name').keyup ->
    name = $('#name').val()
    check_input()
    
  $('#config').change ->
    files = $('#config')[0].files
    if (files.length == 1)
      $('#upload-button').prop('disabled', false)
    else
      $('#upload-button').prop('disabled', true)
      
    
  jobId = location.pathname.split('/').pop()

  $('#save-button').click ->
    name = $('#name').val()
    config = $('#configdata').val()
    $.ajax
      url: '/job/info/' + jobId,
      type: 'PUT',
      contentType: 'text/json',
      data: JSON.stringify({"name": name, "config": config}),
      success: (data, textStatus, jqXHR) ->
        alert('Job successfully updated.'+data.jobId)
        jump = '/dashboard/job/' + data.jobId
        location.replace(jump)
        

  $('#upload').click ->
    action = '/job/config/' + jobId
    $('#uploadform').attr('action', action)
    $('#uploadform').attr('method', 'POST')
    $('#uploadform').submit()
    
  $('#delete-job').click ->
    $.ajax
      url: '/job/' + jobId,
      type: 'DELETE',
      success: (data, textStatus, jqXHR) ->
        jump = '/dashboard/job/list'
        location.replace(jump)

  $('#execjob').click ->
    jobName = $('input[name="name"]').val()
    url = '/job/exec/' + jobId
    $.ajax
      url: url,
      type: 'POST',
      success: (data) ->
        location.reload()

