$ ->
  $table = $('#table')

  getHeight = ->
    $(window).height() - $('h1').outerHeight(true)

  $ ->
    $table.bootstrapTable
      height: getHeight()
      columns: [ [
        {
          title: 'Status'
          field: 'status'
          align: 'center'
          valign: 'middle'
        }
        {
          title: 'Name'
          field: 'name'
          align: 'center'
          valign: 'middle'
        }
        {
          title: 'Progress'
          field: 'progress'
          align: 'center'
          valign: 'middle'
        }
      ] ]