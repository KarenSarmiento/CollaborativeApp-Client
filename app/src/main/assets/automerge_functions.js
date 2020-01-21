/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 */

const updateTypes = {
    ADD_TODO: "addTodo",
    REMOVE_TODO: "removeTodo",
    UPDATE_COMPLETED: "setTodoCompleted"
}

let log = function (text) { document.getElementById('output').textContent += "> " + text + "\n" }

let doc = Automerge.from({ cards: [] })

let applyJsonUpdate = function(jsonString) {
    jsonUpdate = JSON.parse(jsonString)
    switch(jsonUpdate.updateType) {
        case updateTypes.ADD_TODO:
            addCard(jsonUpdate.update.label, false)
            break;
        case updateTypes.REMOVE_TODO:
            removeCard()
            break;
        case updateTypes.UPDATE_COMPLETED:
            setCardCompleted(jsonUpdate.update.index, jsonUpdate.update.completed)
            break;
    }

}

let addCard = function (title, completed) {
    eventName = "automerge_addCard"
    ktchannel.startEvent(eventName)

    doc = Automerge.change(doc, 'Add card', it => {
        it.cards.push({ title: title, completed:completed })
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)

    updateMessage = {
        "updateType" : "addTodo",
        "update" : {
            "label" : title
        }
    }
    return updateMessage
}

let removeCard = function () {
    eventName = "automerge_removeCard"
    ktchannel.startEvent(eventName)

    doc = Automerge.change(doc, 'Delete card', it => {
      delete it.cards[0]
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)

    updateMessage = {
        "updateType" : "removeTodo",
        "update" : {}
    }
    return updateMessage
}

let setCardCompleted = function (index, completed) {
    eventName = "automerge_setCardCompleted"
    ktchannel.startEvent(eventName)

    doc = Automerge.change(doc, 'Set Completed', it => {
      it.cards[index].completed = completed
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)

    updateMessage = {
        "updateType" : "setTodoCompleted",
        "update" : {
            "index" : index,
            "completed" : completed
        }
    }
    return updateMessage
}
