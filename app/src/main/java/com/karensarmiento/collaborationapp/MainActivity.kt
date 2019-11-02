package com.karensarmiento.collaborationapp

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var automerge: Automerge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The callback will by default be called by a BG thread; therefore we need to dispatch it
        // to the UI thread
        automerge = Automerge(webview) {
            runOnUiThread { updateCards(it) }
        }

        button_add_card.setOnClickListener {
            automerge?.addCard(Card(text_field.text.toString(), false))
        }

        button_remove_card.setOnClickListener {
            automerge?.removeCard()
        }
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_cards.indexOfChild(((view.getParent() as ViewGroup).getParent() as ViewGroup))
            automerge?.setCardCompleted(index, view.isChecked)
        }
    }

    /**
     * Will be called whenever the Automerge JS integration deems that the number of cards changed.
     *
     * TODO: This is currently quite inefficient as the entire layout gets re-created.
     * Better would be e.g. using a recyclerview with a proper adapter
     */
    private fun updateCards(cards: List<Card>) {
        layout_cards.removeAllViewsInLayout()
        cards.forEach { card ->
            val view = layoutInflater.inflate(R.layout.card, layout_cards, false)
            val item = view.findViewById<CheckBox>(R.id.checkbox)
            if (item is CheckBox) {
                item.text = card.title
                item.isChecked = card.completed
            }
            layout_cards.addView(view)
        }
    }
}
