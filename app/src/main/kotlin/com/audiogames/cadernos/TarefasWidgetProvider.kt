package com.audiogames.cadernos

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * O widget que fica na tela inicial - mostra as tarefas de hoje, e tem
 * atalho pra criar caderno/página sem precisar abrir o app primeiro.
 */
class TarefasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (idWidget in appWidgetIds) {
            atualizarUmWidget(context, appWidgetManager, idWidget)
        }
    }

    companion object {
        /** Chamado de fora (depois de criar caderno/página) pra forçar todos os widgets a atualizarem na hora. */
        fun atualizarTodosOsWidgets(context: Context) {
            val gerenciador = AppWidgetManager.getInstance(context)
            val ids = gerenciador.getAppWidgetIds(android.content.ComponentName(context, TarefasWidgetProvider::class.java))
            for (id in ids) {
                atualizarUmWidget(context, gerenciador, id)
            }
        }

        private fun atualizarUmWidget(context: Context, appWidgetManager: AppWidgetManager, idWidget: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tarefas)

            // Botão "+ Novo caderno" - abre o app já com o diálogo de novo caderno.
            val intentNovoCaderno = Intent(context, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("abrirNovoCaderno", true)
            }
            views.setOnClickPendingIntent(
                R.id.widget_botao_novo_caderno,
                android.app.PendingIntent.getActivity(context, 1, intentNovoCaderno, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            )

            // Botão "+ Página" e toque no corpo do widget - abrem o Dashboard normal.
            val intentDashboard = Intent(context, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingDashboard = android.app.PendingIntent.getActivity(context, 2, intentDashboard, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_botao_nova_pagina, pendingDashboard)
            views.setOnClickPendingIntent(R.id.widget_titulo, pendingDashboard)

            appWidgetManager.updateAppWidget(idWidget, views)

            // Busca as tarefas de hoje em segundo plano (não dá pra fazer
            // chamada de rede direto aqui - onUpdate() precisa retornar rápido).
            Thread {
                try {
                    val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                    val resultado = ApiClientSincrono.get("resumo/dia.php", mapOf("data" to hoje))
                    val tarefas = resultado?.optJSONArray("tarefas")

                    val viewsAtualizado = RemoteViews(context.packageName, R.layout.widget_tarefas)
                    viewsAtualizado.setOnClickPendingIntent(R.id.widget_botao_novo_caderno,
                        android.app.PendingIntent.getActivity(context, 1, intentNovoCaderno, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
                    viewsAtualizado.setOnClickPendingIntent(R.id.widget_botao_nova_pagina, pendingDashboard)
                    viewsAtualizado.setOnClickPendingIntent(R.id.widget_titulo, pendingDashboard)

                    if (tarefas == null) {
                        viewsAtualizado.setTextViewText(R.id.widget_status, "Não consegui carregar. Toque pra abrir o app.")
                    } else if (tarefas.length() == 0) {
                        viewsAtualizado.setTextViewText(R.id.widget_status, "Nenhuma tarefa hoje 🎉")
                    } else {
                        val quantidade = tarefas.length()
                        viewsAtualizado.setTextViewText(
                            R.id.widget_status,
                            if (quantidade == 1) "1 tarefa hoje:" else "$quantidade tarefas hoje:"
                        )

                        val idsLinhas = listOf(R.id.widget_tarefa_1, R.id.widget_tarefa_2, R.id.widget_tarefa_3)
                        for ((indice, idLinha) in idsLinhas.withIndex()) {
                            if (indice < quantidade && indice < 3) {
                                val item = tarefas.getJSONObject(indice)
                                val textoLinha = if (quantidade > 3 && indice == 2) {
                                    "• ${item.optString("titulo")}  (+${quantidade - 2} mais)"
                                } else {
                                    "• ${item.optString("titulo")}"
                                }
                                viewsAtualizado.setTextViewText(idLinha, textoLinha)
                                viewsAtualizado.setViewVisibility(idLinha, View.VISIBLE)
                            } else {
                                viewsAtualizado.setViewVisibility(idLinha, View.GONE)
                            }
                        }
                    }

                    appWidgetManager.updateAppWidget(idWidget, viewsAtualizado)
                } catch (e: Exception) {
                    // Se der erro (sem internet, sem login, etc.), deixa o
                    // widget do jeito que já estava - não trava nada.
                }
            }.start()
        }
    }
}
