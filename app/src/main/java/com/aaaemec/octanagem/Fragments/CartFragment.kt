package com.aaaemec.octanagem.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaaemec.octanagem.Adapter.CartAdapter
import com.aaaemec.octanagem.Adapter.ProductAdapter
import com.aaaemec.octanagem.MainActivity
import com.aaaemec.octanagem.Model.Cart
import com.aaaemec.octanagem.Model.Produtos
import com.aaaemec.octanagem.R
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.mercadopago.android.px.configuration.PaymentConfiguration
import com.mercadopago.android.px.core.MercadoPagoCheckout
import com.mercadopago.android.px.core.PaymentProcessor
import com.mercadopago.android.px.internal.features.payment_result.PaymentResultActivity
import com.mercadopago.android.px.internal.features.plugins.PaymentProcessorActivity
import com.mercadopago.android.px.internal.util.JsonUtil
import com.mercadopago.android.px.model.Payment
import com.mercadopago.android.px.model.PaymentResult
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode.valueOf
import com.mercadopago.android.px.model.PaymentResult.Builder as PaymentResultBuilder


@Suppress("NAME_SHADOWING")
class CartFragment : Fragment() {
    lateinit var tvTitle: TextView
    lateinit var tvPrice: TextView
    lateinit var iv: ImageView
    lateinit var list: ArrayList<Cart>
    val uid = FirebaseAuth.getInstance().uid ?: ""
    var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    lateinit var RecyclerView: RecyclerView
    lateinit var adapter: CartAdapter
    lateinit var email: String
    lateinit var nome: String
    lateinit var user_id: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater
            .inflate(
                R.layout.cart_fragment,
                container,
                false
            )

        val V: View = inflater
            .inflate(
                R.layout.cart_items,
                container,
                false
            )

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val value: TextView = v.findViewById(R.id.tv_value)
        val btn: Button = v.findViewById(R.id.btn_finish)
        val remove: Button = V.findViewById(R.id.btn_cartitem)
        val itemJsonArray: JSONArray = JSONArray()
        val emailJsonArray: JSONArray = JSONArray()
        val token = "TEST-e287ed41-dee0-4d74-9039-dd68cf6f685e"

        list = arrayListOf()
        RecyclerView = v.findViewById(R.id.rv_cart)
        RecyclerView.setHasFixedSize(true)
        RecyclerView.layoutManager = LinearLayoutManager(context)


        db.collection("Carrinhos").document(uid).collection("Produto").get()
            .addOnSuccessListener { document ->
                var total = 0
                var x = 0

                for (document in document) {
                    val title = document.getString("title")
                    val price = document.getString("price")
                    val img = document.getString("thumbnail")
                    val id = document.getLong("id")
                    val valor = document.getString("valor")

                    val m = Cart(title!!, price!!, img!!, id!!, valor!!)
                    list.add(m)


                    val valorl = Integer.valueOf(valor)
                    val itemJSON: JSONObject = JSONObject()


                    itemJSON.put("title", title)
                    itemJSON.put("picture_url", img.toString())
                    itemJSON.put("quantity", 1)
                    itemJSON.put("currency_id", "BRL")
                    itemJSON.put("unit_price", valorl)

                    itemJsonArray.put(itemJSON)

                    val sum = Integer.valueOf(valor)
                    total += sum
                    Log.d("Tag", "existe: $title")
                    Log.d("Tag", "items: $itemJSON")
                    Log.d("Tag", "items na Array: $itemJsonArray")
                    Log.d("Tag", "items na list: $list")

                }

                value.text = addMask(total.toString(), "R$#####")
                val adapter = CartAdapter(activity!!, list)
                RecyclerView.adapter = adapter
                Log.d("Tag", "Tamanho da lista: ${list.size}")

            }

        db.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                email = document.getString("email").toString()
                user_id = document.getString("uid").toString()
            }



        remove.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                adapter.notifyDataSetChanged()
            }

        })

        btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                val url =
                    "https://api.mercadopago.com/checkout/preferences?access_token=TEST-8287893393395202-120104-07fb1f3b9d80e125fc9079d68475fdb1-240461056"


                val queue = Volley.newRequestQueue(activity)

                val strJson = "{\n" +
                        "           \"items\":\n" +
                        itemJsonArray +
                        "           ,\n" +
                        "           \"payer\": {\n" +
                        "               \"email\": \"$email\"\n" +
                        "          ,     \"id\": \"$user_id\"\n" +
                        "           }\n" +
                        "     }"

                val obj = JSONObject(strJson)


                val stringRequest = object : JsonObjectRequest(
                    Method.POST, url, obj,
                    Response.Listener<JSONObject> { response ->
                        val checkoutPreferenceId: String = response.getString("id")
                        val client: String = response.getString("client_id")
                        MercadoPagoCheckout.Builder(token, checkoutPreferenceId)
                            .build()
                            .startPayment(context!!, MainActivity.REQUEST_CODE)




                        Log.d("TAG", "ID: " + checkoutPreferenceId + "\n" + "Client_ id: " + client)
                    },
                    Response.ErrorListener { error ->
                        val erros = error.toString()

                    }


                ) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json"
                        return headers
                    }

                }

                val policy = DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 10,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
                stringRequest.retryPolicy = policy
                queue.add(stringRequest)

                Log.d("TAG", strJson)

                Log.d("TAG", MercadoPagoCheckout.EXTRA_PAYMENT_RESULT)

//                val i = Intent(context, PaymentResultActivity::class.java)
//
//                startActivityForResult(i,1)

            }


        })





        return v
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MainActivity.REQUEST_CODE) {
            if (resultCode == MercadoPagoCheckout.PAYMENT_RESULT_CODE) {
                val payment: Payment =
                    data!!.getSerializableExtra(MercadoPagoCheckout.EXTRA_PAYMENT_RESULT) as Payment

                val collection = db.collection("Carrinhos").document(uid).collection("Produto")

                deleteall(collection, list.size)
                Log.d("TAG", "Resultado" + payment.paymentStatus)
            }else{
                Log.d("TAG", "Resultado: Erro no Result Code "+MercadoPagoCheckout.PAYMENT_RESULT_CODE +" "+resultCode )
            }
        }else{
            Log.d("TAG", "Resultado: Erro no Request Code" )
        }

    }

    fun deleteall(collection: CollectionReference, batchSize: Int) {
        try {
            var delete = 0
            collection.limit(batchSize.toLong())
                .get()
                .addOnCompleteListener {
                    for (document in it.result!!.documents) {
                        document.reference.delete()
                        ++delete
                    }
                }
        } catch (e: Exception) {

        }
    }

    fun addMask(text: String, mask: String): String {
        var formatado: String = ""
        var i = 0
        for (m: Char in mask.toCharArray()) {
            if (m != '#') {
                formatado += m
                continue
            }
            try {
                formatado += text[i]
            } catch (e: Exception) {
                break
            }
            i++
        }
        return formatado
    }

    override fun onStop() {
        adapter = CartAdapter(activity!!, list)
        RecyclerView.adapter = adapter
        super.onStop()

    }

    override fun onDestroyView() {
        adapter = CartAdapter(activity!!, list)
        RecyclerView.adapter = adapter
        super.onDestroyView()

    }
}


