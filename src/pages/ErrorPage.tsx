import ClientErrorView from '../components/error/ClientErrorView'
import ServerErrorView from '../components/error/ServerErrorView'
import { useErrorPage } from '../hooks/useErrorPage'
import type { ErrorPageProps } from '../types/common'

const ErrorPage: React.FC<ErrorPageProps> = ({ errorCode }) => {
  const { code, handleClickBack } = useErrorPage({ errorCode })

  if (code >= 500) {
    return <ServerErrorView />
  }

  if (code >= 400) {
    return <ClientErrorView onClickBack={handleClickBack} />
  }
  return null
}

export default ErrorPage
