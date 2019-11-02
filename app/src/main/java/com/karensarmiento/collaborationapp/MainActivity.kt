package com.karensarmiento.collaborationapp

import android.os.Bundle
import android.widget.TextView
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
            automerge?.addCard(Card(text_field.text.toString()))
        }

        button_remove_card.setOnClickListener {
            automerge?.removeCard()
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
            view.findViewById<TextView>(R.id.text_title).text = card.title
            layout_cards.addView(view)
        }
    }
}
