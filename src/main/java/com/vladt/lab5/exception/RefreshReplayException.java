package com.vladt.lab5.exception;

/**
 * Проверяемое исключение: по правилам отката @Transactional оно не откатывает
 * транзакцию, поэтому перевод повторно предъявленной сессии в REVOKED сохраняется,
 * хотя HTTP-ответ будет 401 без новой пары токенов.
 */
public class RefreshReplayException extends Exception {
}
