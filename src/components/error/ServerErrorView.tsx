import ServerError from '/public/serverError.svg'

const ServerErrorView = () => {
  return (
    <div className="flex flex-col items-center justify-center h-full">
      <img src={ServerError} alt="Server Error" />
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
}

export default ServerErrorView
