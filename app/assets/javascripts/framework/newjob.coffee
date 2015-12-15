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

  $('#save-button').click ->
    name = $('#name').val()
    config = $('#configdata').val()
    $.ajax
      url: '/job/info',
      type: 'POST',
      contentType: 'text/json',
      data: JSON.stringify({"name": name, "config":config}),
      success: (data, textStatus, jqXHR) ->
        jump = '/dashboard/job/' + data.jobId
        location.replace(jump)

  $('#upload').click ->
    action = '/job/config'
    $('#uploadform').attr('action', action)
    $('#uploadform').attr('method', 'POST')
    $('#uploadform').submit()