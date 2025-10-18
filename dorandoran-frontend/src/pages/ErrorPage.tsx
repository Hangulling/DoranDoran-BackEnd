import { useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import ServerError from '/public/serverError.svg'
import ClientError from '/public/clientError.svg'
import Arrow from '../assets/icon/leftArrow.svg?react'

interface ErrorPageProps {
  errorCode: number
}

const ErrorPage: React.FC<ErrorPageProps> = ({ errorCode }) => {
  const navigate = useNavigate()

  const handleClick = () => {
    navigate('/')
  }

  if (errorCode >= 500) {
    return (
      <div className="flex flex-col items-center justify-center h-full">
        <img src={ServerError} />
        <h2 className="text-[22px] font-semibold text-gray-400 mt-[14px] mb-3 text-center">
          Uh-oh!
          <br />
          Something went wrong.
        </h2>

        <p className="text-gray-300 text-[14px] mb-[30px] text-center leading-relaxed">
          Don’t worry, it’s not your fault.
          <br />
          We’re already looking into it,
          <br />
          so please check back later.
        </p>
        <div className="text-title text-gray-300 text-[14px] text-center">
          Error Code : <span className="text-body">500</span>
        </div>
      </div>
    )
  } else if (errorCode >= 400) {
    return (
      <div className="flex flex-col items-center justify-center h-full">
        <img src={ClientError} />
        <h2 className="text-[22px] font-semibold text-gray-400 mt-10 mb-3 text-center">
          Uh-oh!
          <br />
          Looks like this page got lost.
        </h2>

        <p className="text-gray-300 text-[14px] mb-[51px] text-center leading-relaxed">
          The path you’re trying to reach isn’t here right now.
          <br />
          Please head back to the homepage.
        </p>
        <div className="text-title text-gray-300 text-[14px] text-center mb-[30px]">
          Error Code : <span className="text-body">400</span>
        </div>
        <Button variant="home" className="py-2 pl-[14px] pr-2" onClick={handleClick}>
          <span className="flex items-center text-[14px] gap-x-0">
            Go Back
            <Arrow className="rotate-180 w-[18px] h-[18px]" />
          </span>
        </Button>
      </div>
    )
  }
}

export default ErrorPage
