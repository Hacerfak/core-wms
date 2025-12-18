import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';

/**
 * Componente de Controle de Acesso (RBAC)
 * @param {string} I - A permissão necessária (ex: "RECEBIMENTO_CRIAR")
 * @param {ReactNode} children - O que será renderizado se tiver permissão
 * @param {ReactNode} elseShow - (Opcional) O que mostrar caso NÃO tenha permissão (ex: mensagem de erro ou botão desabilitado)
 */
const Can = ({ I, children, elseShow = null }) => {
    const { userCan } = useContext(AuthContext);

    if (userCan(I)) {
        return children;
    }

    return elseShow;
};

export default Can;