package client.window.tab.panel

import java.awt.{Color, Dimension}

import client.ClientSession.SessionVars
import client.window.tab.DuelTab
import common.game.GameSteps

import scala.collection.mutable.ArrayBuffer
import scala.swing.event.ButtonClicked
import scala.swing.{Button, FlowPanel, Label}

/**
 * Created by HugoSousa on 08-09-2014.
 */
class StepPanel(val duelTab: DuelTab) extends FlowPanel{
  minimumSize = new Dimension(600,50)
  private val playersTurn = new Label(duelTab.players(0) + "'s turn")
  private val main1stPhase = new Button("Main"){ enabled = false }
  private val attackPhase = new Button("Attack"){ enabled = false }
  private val defendPhase = new Button("Defend"){ enabled = false }
  private val battlePhase = new Button("Battle"){ enabled = false }
  private val main2ndPhase = new Button("Main"){ enabled = false }
  private val highlightColor = new Color(50,50,50)

  background = Color.WHITE
  playersTurn.background = Color.WHITE
  main1stPhase.background = highlightColor
  attackPhase.background = Color.WHITE
  defendPhase.background = Color.WHITE
  battlePhase.background = Color.WHITE
  main2ndPhase.background = Color.WHITE

  contents += playersTurn
  contents += main1stPhase
  contents += attackPhase
  contents += defendPhase
  contents += battlePhase
  contents += main2ndPhase

  private val nextStep = new Button("Next Step"){
    reactions +={
      case ButtonClicked(b) =>
        SessionVars.serverHandler.Duel.nextStep(duelTab.gameID)

    }
  }
  private val attack = new Button("Attack"){
    visible = false
    reactions +={
      case ButtonClicked(b) =>
        duelTab.currentStep match{
          case GameSteps.COMBAT_Attack =>
            val summonIDsBuffer = new ArrayBuffer[Int]()
            duelTab.internalGamePanel.playerPanel(duelTab.username).battleFieldPanel.cards.foreach(
              c=> if(c.attacking) summonIDsBuffer += c.remoteCard.id
            )
            val summonIDs = new Array[Int](summonIDsBuffer.size)
            summonIDsBuffer.indices.foreach(i=>summonIDs(i) = summonIDsBuffer(i))
            SessionVars.serverHandler.Duel.setAttackers(duelTab.gameID,summonIDs)
            visible = false
            repaint()

          case _ =>
        }
    }
  }
  private val defend = new Button("Defend"){
    visible = false
    reactions +={
      case ButtonClicked(b) =>
        duelTab.currentStep match{
          case GameSteps.COMBAT_Defend =>
            val defenses = new Array[(Int, Int)](duelTab.defenseLines.defenses.size())
            for(i<-defenses.indices)
              defenses(i) = (new Integer(duelTab.defenseLines.defenses.get(i)._1.remoteCard.id) , new Integer(duelTab.defenseLines.defenses.get(i)._2.remoteCard.id))
            SessionVars.serverHandler.Duel.setDefenses(duelTab.gameID,defenses)
            visible = false
            repaint()
        }
      case _ =>
    }
  }
  private val nextTurn = new Button("End Turn"){
    reactions +={
      case ButtonClicked(b) =>
        duelTab.internalGamePanel.playerPanel(duelTab.players(duelTab.activePlayerIndex)).battleFieldPanel.cards.foreach(c=>c.attacking = false)
        SessionVars.serverHandler.Duel.nextTurn(duelTab.gameID)
    }
  }
  if(duelTab.players(0)!=duelTab.username){
    nextStep.visible = false
    nextTurn.visible = false
  }
  contents += nextStep
  contents += attack
  contents += defend
  contents += nextTurn

  def nextStep(step: GameSteps, ownTurn: Boolean){
    main1stPhase.background = Color.WHITE
    attackPhase.background = Color.WHITE
    defendPhase.background = Color.WHITE
    battlePhase.background = Color.WHITE
    main2ndPhase.background = Color.WHITE
    step match{
      case GameSteps.MAIN_1st =>
        main1stPhase.background = highlightColor

      case GameSteps.MAIN_2nd =>
        main2ndPhase.background = highlightColor
        if(ownTurn){
          nextTurn.visible = true
        }
        if(!ownTurn && defend.visible)
          defend.visible = false


      case GameSteps.COMBAT_Attack =>
        attackPhase.background = highlightColor
        if(ownTurn){
          nextStep.visible = false
          attack.visible = true
        }

      case GameSteps.COMBAT_Defend =>
        defendPhase.background = highlightColor
        if(!ownTurn){
          defend.visible = true
        }

      case GameSteps.COMBAT_Battle =>
        defendPhase.background = highlightColor

      case _ =>
    }
    repaint()
  }

  def nextTurn(ownTurn: Boolean){
    nextStep.visible = ownTurn
    nextTurn.visible = ownTurn
  }

  def resetTurnOwner(){
    playersTurn.text = duelTab.players(duelTab.activePlayerIndex) + "'s turn"
    playersTurn.repaint()
  }
}