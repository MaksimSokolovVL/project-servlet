package com.tictactoe;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;


@WebServlet(name = "LogicServlet", value = "/logic")
public class LogicServlet extends HttpServlet {

    // TODO: Request запроса --- Response ответ
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Получаем текущую сессию
        HttpSession currentSession = req.getSession();


        // Получаем объект игрового поля из сессии
        Field field = extractField(currentSession);

        IsGame isGame = extractIsGame(currentSession);


        // получаем индекс ячейки, по которой произошел клик
        int index = getSelectedIndex(req, resp, currentSession, field, isGame);
        if (index < 0) {
            return;
        }
        Sign currentSign = field.getField().get(index);

        // Проверяем, что ячейка, по которой был клик пустая.
        // Иначе ничего не делаем и отправляем пользователя на ту же страницу без изменений
        // параметров в сессии
        if (Sign.EMPTY != currentSign) {
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/index.jsp");
            dispatcher.forward(req, resp);
            return;
        }

        if (!checkWin(resp, currentSession, field)) {
            // ставим крестик в ячейке, по которой кликнул пользователь
            field.getField().put(index, Sign.CROSS);
        } else {
            return;
        }


        // Проверяем, не победил ли крестик после добавления последнего клика пользователя
        if (checkWin(resp, currentSession, field)) {
            return;
        }

        // Получаем пустую ячейку поля
        int emptyFieldIndex = field.getEmptyFieldIndex();

        if (emptyFieldIndex >= 0) {
            field.getField().put(emptyFieldIndex, Sign.NOUGHT);
            // Проверяем, не победил ли нолик после добавление последнего нолика
            if (checkWin(resp, currentSession, field)) {
                return;
            }
        } else {   // Если пустой ячейки нет и никто не победил - значит это ничья
            // Добавляем в сессию флаг, который сигнализирует что произошла ничья
            currentSession.setAttribute("draw", true);
            isGame.setIsGame(false);


            // Считаем список значков
            List<Sign> data = field.getFieldData();

            // Обновляем этот список в сессии
            currentSession.setAttribute("data", data);

            // Шлем редирект
            resp.sendRedirect("/index.jsp");
            return;

        }

        // Считаем список значков
        List<Sign> data = field.getFieldData();

        // Обновляем объект поля и список значков в сессии
        currentSession.setAttribute("data", data);
        currentSession.setAttribute("field", field);

        resp.sendRedirect("/index.jsp");
    }


    //получим объект “field” типа Field из сессии
    private Field extractField(HttpSession currentSession) {
        Object fieldAttribute = currentSession.getAttribute("field");
        if (Field.class != fieldAttribute.getClass()) {
            currentSession.invalidate();
            throw new RuntimeException("Session is broken, try one more time");
        }
        return (Field) fieldAttribute;
    }


    private IsGame extractIsGame(HttpSession currentSession) {
        Object fieldAttribute = currentSession.getAttribute("isGameObj");
        if (IsGame.class != fieldAttribute.getClass()) {
            currentSession.invalidate();
            throw new RuntimeException("Session is broken, try one more time");
        }
        return (IsGame) fieldAttribute;
    }

    //при клике по любой ячейке, мы будем на сервере получать индекс этой ячейки
    private int getSelectedIndex(HttpServletRequest request, HttpServletResponse response, HttpSession session, Field field, IsGame isGame) throws IOException {
        String click = request.getParameter("click");
        boolean isNumeric = click.chars().allMatch(Character::isDigit);

        boolean isWin = checkWin(response, session, field);
        boolean isDraw = checkDraw(response, session, field, isGame);

        if (isWin || isDraw) {
            return -1;
        }
        return isNumeric ? Integer.parseInt(click) : 0;
    }

    /**
     * Метод проверяет, нет ли трех крестиков/ноликов в ряд.
     * Возвращает true/false
     */
    private boolean checkWin(HttpServletResponse response, HttpSession currentSession, Field field) throws IOException {
        Sign winner = field.checkWin();
        if (Sign.CROSS == winner || Sign.NOUGHT == winner) {
            // Добавляем флаг, который показывает что кто-то победил
            currentSession.setAttribute("winner", winner);
            workingWithListsSession(response, currentSession, field);
            return true;
        }
        return false;
    }

    private boolean checkDraw(HttpServletResponse response, HttpSession currentSession, Field field, IsGame isGame) throws IOException {

        if (!isGame.getIsGame()) {
            workingWithListsSession(response, currentSession, field);
            return true;
        }
        return false;
    }

    private void workingWithListsSession(HttpServletResponse response, HttpSession currentSession, Field field) throws IOException {
        // Считаем список значков
        List<Sign> data = field.getFieldData();

        // Обновляем этот список в сессии
        currentSession.setAttribute("data", data);

        // Шлем редирект
        response.sendRedirect("/index.jsp");
    }
}