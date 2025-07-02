return Multi.createFrom().publisher(index.query(request))
    .onItem().transformToMulti(page -> Multi.createFrom().iterable(page.items()))
    .concatenate();